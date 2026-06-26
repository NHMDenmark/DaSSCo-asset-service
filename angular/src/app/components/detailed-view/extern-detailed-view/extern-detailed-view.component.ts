import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {BehaviorSubject, filter, finalize, map, shareReplay, switchMap, take, tap} from 'rxjs';
import {
  ExternalAssetMetadataState,
  ExternDetailedViewService
} from '../../../services/extern-detailed-view.service';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {WikiPageUrl} from '../../../utility';
import {Dialog} from '@angular/cdk/dialog';
import {AssetThumbnailModalComponent} from '../asset-thumbnail-modal/asset-thumbnail-modal.component';
import {AssetBundleDownloadService} from '../../../services/asset-bundle-download.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {AuthService} from '../../../services/auth.service';

type ExternalAssetViewState = ExternalAssetMetadataState & {assetGuid: string};
type VisibleExternalAssetMetadataState = Extract<ExternalAssetViewState, {status: 'visible'}>;

@Component({
  selector: 'dassco-extern-detailed-view',
  styleUrls: ['./extern-detailed-view.component.scss', '../detailed-view.component.scss'],
  templateUrl: './extern-detailed-view.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExternDetailedViewComponent {
  private route = inject(ActivatedRoute);
  private sanitizer = inject(DomSanitizer);
  private cdkDialog = inject(Dialog);
  private assetBundleDownloadService = inject(AssetBundleDownloadService);
  private snackBar = inject(MatSnackBar);
  private authService = inject(AuthService);
  wikiPageUrl = inject(WikiPageUrl);
  baseUrl = window.location.origin;
  externDetailedViewService = inject(ExternDetailedViewService);
  assetGuid$ = this.route.paramMap.pipe(map((params) => params.get('asset_guid')));
  private loading = new BehaviorSubject(true);
  loading$ = this.loading.asObservable();
  private loadingImage = new BehaviorSubject(true);
  loadingImage$ = this.loadingImage.asObservable();
  private downloading = new BehaviorSubject(false);
  downloading$ = this.downloading.asObservable();

  assetState$ = this.assetGuid$.pipe(
    filter((assetGuid): assetGuid is string => assetGuid !== null && assetGuid !== undefined),
    tap(() => {
      this.loading.next(true);
      this.loadingImage.next(true);
    }),
    switchMap((assetGuid) =>
      this.externDetailedViewService.getAssetMetaData(assetGuid).pipe(
        map((assetState) => ({...assetState, assetGuid}) as ExternalAssetViewState),
        tap((assetState) => {
          this.loading.next(false);
          if (assetState.status !== 'visible') {
            this.loadingImage.next(false);
          }
        })
      )
    ),
    shareReplay({bufferSize: 1, refCount: true})
  );

  assetMetaData$ = this.assetState$.pipe(
    filter((assetState): assetState is VisibleExternalAssetMetadataState => assetState.status === 'visible'),
    map((assetState) => assetState.asset)
  );

  thumbnail$ = this.assetMetaData$.pipe(
    filter((asset) => asset !== null && asset !== undefined),
    switchMap((asset) =>
      this.externDetailedViewService.getThumbnail(asset?.institution, asset?.collection, asset?.asset_guid).pipe(
        map((value) => {
          if (!value) return value;
          const objectUrl = URL.createObjectURL(value);
          return this.sanitizer.bypassSecurityTrustUrl(objectUrl);
        }),
        tap({
          next: () => {
            this.loadingImage.next(false);
          }
        })
      )
    )
  );
  assetFileList$ = this.assetMetaData$.pipe(
    filter((asset) => asset !== null && asset !== undefined),
    switchMap((asset) =>
      this.externDetailedViewService
        .getAssetFileList(asset?.asset_guid)
        .pipe(map((files) => files.map((f) => f.split('/').pop() ?? '')))
    )
  );

  downloadCsv(assetGuid: string | undefined): void {
    if (!assetGuid) return;
    this.downloading.next(true);
    this.snackBar.open('Preparing CSV download...', undefined, {duration: 3000});
    this.externDetailedViewService
      .downloadMetadataCsv(assetGuid)
      .pipe(
        take(1),
        finalize(() => {
          this.downloading.next(false);
        })
      )
      .subscribe({
        next: (blob: Blob) => {
          this.externDetailedViewService.triggerDownload(blob, `asset_${assetGuid}.csv`);
          this.snackBar.open('CSV file has been downloaded.', undefined, {duration: 3000});
        },
        error: (err) => {
          console.error('CSV download failed', err);
          this.snackBar.open('There has been an error downloading the CSV file.', undefined, {duration: 5000});
        }
    });
  }

  loginToDetailedView(assetGuid: string | undefined): void {
    if (!assetGuid) return;
    sessionStorage.setItem('postLoginUrl', `/detailed-view/${assetGuid}`);
    this.authService.login();
  }

  downloadBundle(assetGuid: string | undefined): void {
    if (!assetGuid) return;

    this.assetBundleDownloadService.startBundleDownload([assetGuid], {
      access: 'external'
    });
  }

  isBundleDownloadPreparing(assetGuid: string | undefined): boolean {
    if (!assetGuid) return false;
    return this.assetBundleDownloadService.isBundleInProgress([assetGuid], 'external');
  }

  openAssetThumbnailModal(thumbnailUrl: SafeUrl) {
    if (!thumbnailUrl) {
      return;
    }
    this.cdkDialog.open(AssetThumbnailModalComponent, {
      data: thumbnailUrl
    });
  }

  trackBy(_index: number, guid: string) {
    return guid;
  }
}
