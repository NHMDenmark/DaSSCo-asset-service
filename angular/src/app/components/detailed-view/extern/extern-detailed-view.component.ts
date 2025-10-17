import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {BehaviorSubject, filter, map, switchMap, tap} from 'rxjs';
import {ExternDetailedViewService} from '../../../services/extern-detailed-view.service';
import {DomSanitizer} from '@angular/platform-browser';
import {WikiPageUrl} from "../../../utility";

@Component({
  selector: 'dassco-extern-detailed-view',
  styleUrls: ['./extern-detailed-view.component.scss', '../detailed-view.component.scss'],
  templateUrl: './extern-detailed-view.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExternDetailedViewComponent {
  private route = inject(ActivatedRoute);
  private sanitizer = inject(DomSanitizer);
  wikiPageUrl = inject(WikiPageUrl);
  externDetailedViewService = inject(ExternDetailedViewService);
  assetGuid$ = this.route.paramMap.pipe(map((params) => params.get('asset_guid')));
  loading = new BehaviorSubject(true);
  loading$ = this.loading.asObservable();
  loadingImage = new BehaviorSubject(true);
  loadingImage$ = this.loadingImage.asObservable();

  assetMetaData$ = this.assetGuid$.pipe(
    filter((assetGuid) => assetGuid !== null && assetGuid !== undefined),
    switchMap((assetGuid) =>
      this.externDetailedViewService
        .getAssetMetaData(assetGuid as string)
        .pipe(tap({next: () => this.loading.next(false)}))
    )
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
            this.loadingImage.next(false)
          },
        })
      )
    )
  );

  trackByGuid(_index: number, guid: string) {
    return guid;
  }

}
