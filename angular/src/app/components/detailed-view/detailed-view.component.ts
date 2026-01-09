import {Component, ElementRef, inject, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {DetailedViewService} from '../../services/detailed-view.service';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Asset, ExternalPublisher, Issue} from '../../types/types';
import {QueryToOtherPages} from '../../services/query-to-other-pages.service';
import {BehaviorSubject, combineLatest, EMPTY, filter, map, Subject, switchMap, takeUntil} from 'rxjs';
import {DatePipe} from '@angular/common';
import {WikiPageUrl} from '../../utility';
import {MatDialog} from '@angular/material/dialog';
import {IssueViewerComponent} from '../issue-viewer/issue-viewer.component';
import {isNotUndefined} from '@northtech/ginnungagap';
import {Dialog} from '@angular/cdk/dialog';
import {AssetThumbnailModalComponent} from './asset-thumbnail-modal/asset-thumbnail-modal.component';

@Component({
  selector: 'dassco-detailed-view',
  providers: [DatePipe],
  templateUrl: './detailed-view.component.html',
  styleUrls: ['./detailed-view.component.scss']
})
export class DetailedViewComponent implements OnInit, OnDestroy {
  private detailedViewService = inject(DetailedViewService);
  private sanitizer = inject(DomSanitizer);
  private route = inject(ActivatedRoute);
  private _snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private cdkDialog = inject(Dialog);
  private router = inject(Router);
  private queryToDetailedViewService = inject(QueryToOtherPages);
  private readonly destroy = new Subject<void>();
  @ViewChild('assetMetadata') metadataContainer?: ElementRef<HTMLDivElement>;

  dataLoaded = false;
  datePipe = inject(DatePipe);
  wikiPageUrl = inject(WikiPageUrl);

  baseUrl = window.location.origin;
  assetSubject = new BehaviorSubject<Asset | undefined>(undefined);
  asset$ = this.assetSubject.asObservable();
  specimenBarcodes?: string | undefined;
  fileFormats?: string | undefined;
  restrictedAccess?: string | undefined;
  tags?: string | undefined;
  events?: string[] | undefined;

  thumbnailUrl?: SafeUrl;

  assetFiles = new BehaviorSubject<string[]>([]);
  assetFiles$ = this.assetFiles.asObservable();

  assetGuid = new BehaviorSubject<string>('');
  assetList = new BehaviorSubject<string[]>([]);
  assetList$ = this.assetList.asObservable();
  currentIndex$ = combineLatest([this.assetGuid.asObservable(), this.assetList.asObservable()]).pipe(
    map(([assetGuid, assetList]) => {
      if (assetList && assetGuid) {
        return assetList.indexOf(assetGuid);
      }
      return -1;
    })
  );

  isPreviousDisabled$ = this.currentIndex$.pipe(map((currentIndex) => currentIndex <= 0));
  previousAsset$ = combineLatest([this.assetList.asObservable(), this.currentIndex$]).pipe(
    map(([assetList, currentIndex]) => {
      if (currentIndex <= 0) {
        return undefined;
      }
      return assetList[currentIndex - 1];
    })
  );
  nextAsset$ = combineLatest([this.assetList.asObservable(), this.currentIndex$]).pipe(
    map(([assetList, currentIndex]) => {
      if (assetList.length === currentIndex - 1) {
        return undefined;
      }
      return assetList[currentIndex + 1];
    })
  );

  constructor() {
    this.assetGuid
      .asObservable()
      .pipe(
        takeUntil(this.destroy),
        filter((assetGuid) => assetGuid.length > 0),
        switchMap((assetGuid) => this.detailedViewService.getAssetMetadata(assetGuid))
      )
      .subscribe((assetResponse) => {
        if (assetResponse) {
          this.assetSubject.next(assetResponse);
          this.dataLoaded = true;
          const specimen = (assetResponse?.asset_specimen ?? []).flatMap((a) => a?.specimen ?? []);
          this.specimenBarcodes = specimen.map((s) => s.barcode).join(', ');
          this.fileFormats = assetResponse?.file_formats?.map((file_format) => file_format).join(', ');
          this.restrictedAccess = assetResponse?.restricted_access?.map((type) => type).join(', ');
          this.tags = Object.entries(assetResponse?.tags ?? {})
            .map(([key, value]) => `${key}: ${value}`)
            .join(', ');
          this.events = assetResponse.events?.map(
            (event) =>
              `Event: ${event.event}, Timestamp: ${this.datePipe.transform(
                event?.timestamp?.toString(),
                'dd/MM-yyyy HH:mm'
              )}`
          );
        }
      });

    this.asset$
      .pipe(
        takeUntil(this.destroy),
        filter((asset) => isNotUndefined<Asset>(asset)),
        switchMap((asset) => {
          const assetGuid = asset?.asset_guid;
          if (!assetGuid) return EMPTY;
          return this.detailedViewService.getFileList(assetGuid).pipe(
            switchMap((fileList) => {
              if (fileList) {
                this.assetFiles.next(
                  fileList.map((filePath) => {
                    const parts: string[] = filePath.split('/');
                    return parts[parts.length - 1];
                  })
                );
                if (fileList.length > 0) {
                  return this.detailedViewService.getThumbnail(
                    asset?.institution ?? '',
                    asset?.collection ?? '',
                    assetGuid
                  );
                } else {
                  this.assetFiles.next([]);
                  this.thumbnailUrl = '';
                }
                return EMPTY;
              } else {
                this.assetFiles.next([]);
                this.thumbnailUrl = '';
              }
              return EMPTY;
            })
          );
        })
      )
      .subscribe({
        next: (blob) => {
          if (blob) {
            const objectUrl = URL.createObjectURL(blob);
            this.thumbnailUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
          }
        },
        error: (err: Error) => console.log(err)
      });
  }
  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy)).subscribe((params: Params) => {
      this.assetGuid.next(params['asset_guid']);
      this.metadataContainer?.nativeElement?.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    });
    this.assetList.next(this.queryToDetailedViewService.getAssets());
  }
  ngOnDestroy(): void {
    this.destroy.next();
    this.destroy.complete();
  }

  handleAssetNavigation(assetGuid: string) {
    if (!assetGuid) return;
    this.thumbnailUrl = undefined;
    this.assetFiles.next([]);
    this.assetSubject.next(undefined);
    this.dataLoaded = false;
    this.assetGuid.next(assetGuid);
    this.metadataContainer?.nativeElement?.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
    this.assetList.next(this.queryToDetailedViewService.getAssets());
    this.router.navigate(['/detailed-view', assetGuid]);
  }

  downloadCsv() {
    const asset = this.assetSubject.getValue();
    if (!asset?.asset_guid) return;

    const currentAsset: string[] = [asset.asset_guid];
    this.detailedViewService.postCsv(currentAsset).subscribe({
      next: (response) => {
        if (response.status === 200) {
          const guid: string = response.body;
          this.detailedViewService.getFile(guid, 'assets.csv').subscribe({
            next: (data) => {
              const url = window.URL.createObjectURL(data);
              const link = document.createElement('a');
              link.href = url;
              link.download = 'assets.csv';

              document.body.appendChild(link);
              link.click();

              document.body.removeChild(link);
              window.URL.revokeObjectURL(url);

              this.detailedViewService.deleteFile(guid).subscribe({
                next: () => undefined,
                error: () => {
                  this.openSnackBar("There's been an error deleting the CSV file", 'Close');
                }
              });
            },
            error: () => {
              this.openSnackBar('There has been an error downloading the CSV file.', 'Close');
            }
          });
        }
      },
      error: (error) => {
        this.openSnackBar(error.error, 'Close');
      }
    });
  }

  downloadZip() {
    const asset = this.assetSubject.getValue();
    if (!asset?.asset_guid) return;
    const currentAsset: string[] = [asset.asset_guid];
    this.detailedViewService.postCsv(currentAsset).subscribe({
      next: (response) => {
        const guid: string = response.body;
        if (response.status === 200) {
          this.detailedViewService.postZip(guid, currentAsset).subscribe({
            next: (response) => {
              if (response.status === 200) {
                this.detailedViewService.getFile(guid, 'assets.zip').subscribe({
                  next: (data) => {
                    const url = window.URL.createObjectURL(data);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = 'assets.zip';

                    document.body.appendChild(link);
                    link.click();

                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);

                    this.detailedViewService.deleteFile(guid).subscribe({
                      error: () => {
                        this.openSnackBar('There has been an error deleting the ZIP file', 'Close');
                      }
                    });
                  },
                  error: () => {
                    this.openSnackBar('There was an error retrieving the file', 'Close');
                  }
                });
              }
            },
            error: (error) => {
              this.openSnackBar(error.error, 'Close');
            }
          });
        }
      },
      error: (error) => {
        this.openSnackBar(error.error, 'Close');
      }
    });
  }

  openIssueDialog(issue: Issue) {
    this.dialog.open(IssueViewerComponent, {
      maxWidth: '500px',
      data: issue
    });
  }

  openSnackBar(message: string, action: string) {
    this._snackBar.open(message, action);
  }

  openAssetThumbnailModal() {
    if (!this.thumbnailUrl) {
      return;
    }
    this.cdkDialog.open(AssetThumbnailModalComponent, {
      data: this.thumbnailUrl
    });
  }
  trackBy(_index: number, value: string) {
    return value;
  }
  trackByPublicationId(_index: number, value: ExternalPublisher) {
    return value.publication_id;
  }
  trackByIssueId(_index: number, issue: Issue) {
    return issue.issue_id;
  }
}
