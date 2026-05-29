import {HttpClient, HttpErrorResponse, HttpResponse} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {
  BehaviorSubject,
  catchError,
  EMPTY,
  finalize,
  interval,
  Observable,
  of,
  Subject,
  Subscription,
  switchMap,
  takeUntil,
  throwError
} from 'rxjs';
import {FileProxy} from '../utility';

export type AssetBundleJobStatus = 'PREPARING' | 'READY' | 'FAILED';
export type AssetBundleDownloadStatus = AssetBundleJobStatus | 'CANCELLED';

export interface AssetBundleJobResponse {
  jobId: string;
  status: AssetBundleJobStatus;
  totalAssets: number;
  processedAssets: number;
  message: string;
}

export interface AssetBundleDownload {
  id: string;
  assetKey: string;
  assetGuids: string[];
  access: AssetBundleDownloadAccess;
  status: AssetBundleDownloadStatus;
  totalAssets: number;
  processedAssets: number;
  message: string;
  downloadUrl: string;
  cancelling?: boolean;
  cancelStart$?: Subject<void>;
}

export type AssetBundleDownloadAccess = 'internal' | 'external';

export interface AssetBundleDownloadOptions {
  access?: AssetBundleDownloadAccess;
  cancel$?: Observable<unknown>;
}

@Injectable({
  providedIn: 'root'
})
export class AssetBundleDownloadService {
  private readonly oidcSecurityService = inject(OidcSecurityService);
  private readonly http = inject(HttpClient);
  private readonly fileProxyRoot = this.trimTrailingSlash(inject(FileProxy));
  private readonly internalJobsUrl = this.joinFileProxyPath('/file_proxy/api/assetfiles/asset-bundles/jobs');
  private readonly externalJobsUrl = this.joinFileProxyPath('/file_proxy/api/assetfiles/asset-bundles/extern/jobs');
  private readonly downloadsSubject = new BehaviorSubject<AssetBundleDownload[]>([]);
  private readonly pollingSubscriptions = new Map<string, Subscription>();

  readonly downloads$ = this.downloadsSubject.asObservable();

  startBundleDownload(assetGuids: string[], options: AssetBundleDownloadOptions = {}): void {
    const dedupedAssetGuids = Array.from(new Set(assetGuids));
    if (dedupedAssetGuids.length === 0) return;

    const access = options.access ?? 'internal';
    const assetKey = this.getAssetKey(dedupedAssetGuids, access);
    if (this.isBundleInProgress(dedupedAssetGuids, access)) return;

    const cancelStart$ = new Subject<void>();
    const pendingDownload: AssetBundleDownload = {
      id: `pending-${assetKey}`,
      assetKey,
      assetGuids: dedupedAssetGuids,
      access,
      status: 'PREPARING',
      totalAssets: dedupedAssetGuids.length,
      processedAssets: 0,
      message: 'Preparing download from ERDA...',
      downloadUrl: '',
      cancelStart$
    };
    this.upsertDownload(pendingDownload);

    let cancelled = false;
    const startCancelSubscription = options.cancel$?.subscribe(() => {
      cancelled = true;
      this.removeDownload(pendingDownload.id);
    });

    this.postStartJob(dedupedAssetGuids, access)
      .pipe(
        takeUntil(cancelStart$),
        takeUntil(options.cancel$ ?? EMPTY),
        catchError((error) => {
          this.markFailed(assetKey, this.getErrorMessage(error, 'There has been an error preparing the ZIP file.'));
          return EMPTY;
        }),
        finalize(() => {
          startCancelSubscription?.unsubscribe();
          cancelStart$.complete();
        })
      )
      .subscribe((response) => {
        if (cancelled) return;

        if (!response.body) {
          this.markFailed(assetKey, 'There has been an error preparing the ZIP file.');
          return;
        }

        const downloadUrl = this.getDownloadUrl(response, access);
        const download = this.toDownload(response.body, assetKey, dedupedAssetGuids, access, downloadUrl);
        this.upsertDownload(download);
        this.pollUntilComplete(download, options.cancel$);
      });
  }

  isBundleInProgress(assetGuids: string[], access: AssetBundleDownloadAccess = 'internal'): boolean {
    const assetKey = this.getAssetKey(assetGuids, access);
    return this.downloadsSubject.value.some((download) => download.assetKey === assetKey && download.status === 'PREPARING');
  }

  dismiss(download: AssetBundleDownload): void {
    this.pollingSubscriptions.get(download.id)?.unsubscribe();
    this.pollingSubscriptions.delete(download.id);
    this.removeDownload(download.id);
  }

  cancel(download: AssetBundleDownload): void {
    if (download.status !== 'PREPARING') {
      this.dismiss(download);
      return;
    }

    if (download.id.startsWith('pending-')) {
      download.cancelStart$?.next();
      this.dismiss(download);
      return;
    }

    this.pollingSubscriptions.get(download.id)?.unsubscribe();
    this.pollingSubscriptions.delete(download.id);
    this.upsertDownload({...download, cancelling: true});

    this.deleteJob(download)
      .pipe(
        catchError((error: HttpErrorResponse) => {
          if (error.status === 404) {
            this.upsertDownload({
              ...download,
              status: 'CANCELLED',
              message: 'Download preparation no longer exists.',
              cancelling: false
            });
            return EMPTY;
          }

          this.upsertDownload({...download, cancelling: false});
          return EMPTY;
        })
      )
      .subscribe(() => this.removeDownload(download.id));
  }

  private pollUntilComplete(download: AssetBundleDownload, cancel$?: Observable<unknown>): void {
    const stopPolling$ = new Subject<void>();
    const cancelSubscription = cancel$?.subscribe(() => {
      stopPolling$.next();
      this.removeDownload(download.id);
    });

    const pollingSubscription = interval(3000)
      .pipe(
        takeUntil(stopPolling$),
        switchMap(() => this.getStatus(download.id)),
        catchError((error) => {
          this.markFailed(download.assetKey, this.getErrorMessage(error, 'There has been an error preparing the ZIP file.'));
          stopPolling$.next();
          return EMPTY;
        })
      )
      .subscribe((status) => {
        const updatedDownload = this.toDownload(
          status,
          download.assetKey,
          download.assetGuids,
          download.access,
          download.downloadUrl
        );
        this.upsertDownload(updatedDownload);

        if (status.status === 'READY') {
          stopPolling$.next();
          this.downloadReadyBundle(updatedDownload);
        }

        if (status.status === 'FAILED') {
          stopPolling$.next();
        }
      });

    stopPolling$.subscribe(() => {
      pollingSubscription.unsubscribe();
      cancelSubscription?.unsubscribe();
      this.pollingSubscriptions.delete(download.id);
      stopPolling$.complete();
    });

    this.pollingSubscriptions.set(download.id, pollingSubscription);
  }

  private getStatus(jobId: string): Observable<AssetBundleJobResponse> {
    const existingDownload = this.downloadsSubject.value.find((download) => download.id === jobId);
    const access = existingDownload?.access ?? 'internal';
    const jobsUrl = this.getJobsUrl(access);

    if (access === 'external') {
      return this.http.get<AssetBundleJobResponse>(`${jobsUrl}/${jobId}`);
    }

    return this.withAccessToken().pipe(
      switchMap((token) =>
        this.http.get<AssetBundleJobResponse>(`${jobsUrl}/${jobId}`, {
          headers: {'Authorization': 'Bearer ' + token}
        })
      )
    );
  }

  private downloadReadyBundle(download: AssetBundleDownload): void {
    this.getReadyBundle(download)
      .pipe(catchError((error) => {
        this.markFailed(download.assetKey, this.getErrorMessage(error, 'There has been an error downloading the ZIP file.'));
        return EMPTY;
      }))
      .subscribe((response) => {
        const blob = response.body;
        if (!blob) {
          this.markFailed(download.assetKey, 'There has been an error downloading the ZIP file.');
          return;
        }

        const filename = this.getFilename(response) ?? this.getFallbackFilename();
        this.triggerBrowserDownload(blob, filename);
        this.removeDownload(download.id);
      });
  }

  private withAccessToken(): Observable<string> {
    return this.oidcSecurityService.getAccessToken().pipe(
      switchMap((token) => {
        if (!token) return throwError(() => new Error('Missing access token'));
        return of(token);
      })
    );
  }

  private postStartJob(
    assetGuids: string[],
    access: AssetBundleDownloadAccess
  ): Observable<HttpResponse<AssetBundleJobResponse>> {
    const jobsUrl = this.getJobsUrl(access);

    if (access === 'external') {
      return this.http.post<AssetBundleJobResponse>(jobsUrl, assetGuids, {
        headers: {'Content-Type': 'application/json'},
        observe: 'response'
      });
    }

    return this.withAccessToken().pipe(
      switchMap((token) =>
        this.http.post<AssetBundleJobResponse>(jobsUrl, assetGuids, {
          headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
          },
          observe: 'response'
        })
      )
    );
  }

  private getReadyBundle(download: AssetBundleDownload): Observable<HttpResponse<Blob>> {
    if (download.access === 'external') {
      return this.http.get(download.downloadUrl, {
        observe: 'response',
        responseType: 'blob'
      });
    }

    return this.withAccessToken().pipe(
      switchMap((token) =>
        this.http.get(download.downloadUrl, {
          headers: {'Authorization': 'Bearer ' + token},
          observe: 'response',
          responseType: 'blob'
        })
      )
    );
  }

  private deleteJob(download: AssetBundleDownload): Observable<void> {
    const url = `${this.getJobsUrl(download.access)}/${download.id}`;

    if (download.access === 'external') {
      return this.http.delete<void>(url);
    }

    return this.withAccessToken().pipe(
      switchMap((token) =>
        this.http.delete<void>(url, {
          headers: {'Authorization': 'Bearer ' + token}
        })
      )
    );
  }

  private toDownload(
    response: AssetBundleJobResponse,
    assetKey: string,
    assetGuids: string[],
    access: AssetBundleDownloadAccess,
    downloadUrl: string
  ): AssetBundleDownload {
    return {
      id: response.jobId,
      assetKey,
      assetGuids,
      access,
      status: response.status,
      totalAssets: response.totalAssets,
      processedAssets: response.processedAssets,
      message: response.message || this.getDefaultMessage(response.status),
      downloadUrl
    };
  }

  private upsertDownload(download: AssetBundleDownload): void {
    const existing = this.downloadsSubject.value;
    const index = existing.findIndex((current) => current.assetKey === download.assetKey);
    if (index === -1) {
      this.downloadsSubject.next([...existing, download]);
      return;
    }

    const next = [...existing];
    next[index] = {...next[index], ...download};
    this.downloadsSubject.next(next);
  }

  private markFailed(assetKey: string, message: string): void {
    const existing = this.downloadsSubject.value.find((download) => download.assetKey === assetKey);
    if (!existing) return;
    this.upsertDownload({
      ...existing,
      status: 'FAILED',
      message
    });
  }

  private removeDownload(downloadId: string): void {
    this.downloadsSubject.next(this.downloadsSubject.value.filter((current) => current.id !== downloadId));
  }

  private getDownloadUrl(response: HttpResponse<AssetBundleJobResponse>, access: AssetBundleDownloadAccess): string {
    const linkHeader = response.headers.get('Link');
    const downloadUrl = linkHeader
      ?.split(',')
      .map((link) => link.trim())
      .find((link) => link.includes('rel="download"'))
      ?.match(/<([^>]+)>/)?.[1];

    if (downloadUrl) return downloadUrl;

    const jobId = response.body?.jobId;
    return `${this.getJobsUrl(access)}/${jobId}/download`;
  }

  private getFilename(response: HttpResponse<Blob>): string | undefined {
    const disposition = response.headers.get('Content-Disposition');
    return disposition?.match(/filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i)?.slice(1).find(Boolean);
  }

  private triggerBrowserDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = decodeURIComponent(filename);

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }

  private getJobsUrl(access: AssetBundleDownloadAccess): string {
    return access === 'external' ? this.externalJobsUrl : this.internalJobsUrl;
  }

  private joinFileProxyPath(path: string): string {
    return `${this.fileProxyRoot}${path}`;
  }

  private trimTrailingSlash(url: string): string {
    return url.replace(/\/+$/, '');
  }

  private getAssetKey(assetGuids: string[], access: AssetBundleDownloadAccess): string {
    return `${access}:${Array.from(new Set(assetGuids)).sort().join('|')}`;
  }

  private getDefaultMessage(status: AssetBundleDownloadStatus): string {
    if (status === 'CANCELLED') return 'Download preparation no longer exists.';
    if (status === 'FAILED') return 'There has been an error preparing the ZIP file.';
    if (status === 'READY') return 'Download is ready.';
    return 'Preparing download from ERDA...';
  }

  private getFallbackFilename(): string {
    const now = new Date();
    const pad = (value: number) => value.toString().padStart(2, '0');
    const timestamp = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}-${pad(
      now.getHours()
    )}-${pad(now.getMinutes())}-${pad(now.getSeconds())}`;
    return `asset-bundle-${timestamp}.zip`;
  }

  private getErrorMessage(error: any, fallback: string): string {
    return error?.error?.message ?? error?.error ?? error?.message ?? fallback;
  }
}
