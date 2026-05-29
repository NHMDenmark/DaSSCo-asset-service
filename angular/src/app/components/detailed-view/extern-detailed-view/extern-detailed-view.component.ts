import {ChangeDetectionStrategy, Component, inject, OnDestroy} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {BehaviorSubject, filter, finalize, map, Subject, switchMap, take, tap} from 'rxjs';
import {ExternDetailedViewService} from '../../../services/extern-detailed-view.service';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {WikiPageUrl} from '../../../utility';
import {Dialog} from '@angular/cdk/dialog';
import {AssetThumbnailModalComponent} from '../asset-thumbnail-modal/asset-thumbnail-modal.component';
import {AssetBundleDownloadService} from '../../../services/asset-bundle-download.service';

@Component({
  selector: 'dassco-extern-detailed-view',
  styleUrls: ['./extern-detailed-view.component.scss', '../detailed-view.component.scss'],
  templateUrl: './extern-detailed-view.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExternDetailedViewComponent implements OnDestroy {
  private route = inject(ActivatedRoute);
  private sanitizer = inject(DomSanitizer);
  private cdkDialog = inject(Dialog);
  private assetBundleDownloadService = inject(AssetBundleDownloadService);
  private readonly destroy = new Subject<void>();
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

  assetMetaData$ = this.assetGuid$.pipe(
    filter((assetGuid) => assetGuid !== null && assetGuid !== undefined),
    switchMap((assetGuid) =>
      this.externDetailedViewService
        .getAssetMetaData(assetGuid as string)
        .pipe(tap({next: () => this.loading.next(false)}))
    )
  );

  assetFileList = this.assetMetaData$.pipe();

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
        },
        error: (err) => {
          console.error('CSV download failed', err);
        }
      });
  }

  downloadBundle(assetGuid: string | undefined): void {
    if (!assetGuid) return;

    this.assetBundleDownloadService.startBundleDownload([assetGuid], {
      access: 'external',
      cancel$: this.destroy
    });
  }

  isBundleDownloadPreparing(assetGuid: string | undefined): boolean {
    if (!assetGuid) return false;
    return this.assetBundleDownloadService.isBundleInProgress([assetGuid], 'external');
  }

  ngOnDestroy(): void {
    this.destroy.next();
    this.destroy.complete();
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
