import {Component, inject, OnInit} from '@angular/core';
import {DetailedViewService} from '../../services/detailed-view.service';
import {DomSanitizer, SafeUrl} from '@angular/platform-browser';
import {ActivatedRoute, Params} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Asset} from '../../types/types';
import {QueryToOtherPages} from '../../services/query-to-other-pages';
import {EMPTY, switchMap, take} from 'rxjs';
import {DatePipe} from '@angular/common';

@Component({
  selector: 'dassco-detailed-view',
  providers: [DatePipe],
  templateUrl: './detailed-view.component.html',
  styleUrls: ['./detailed-view.component.scss']
})
export class DetailedViewComponent implements OnInit {
  // Asset Guid is retrieved from the URL:
  assetGuid: string = '';
  currentIndex: number = -1;
  assetList: string[] = this.queryToDetailedViewService.getAssets();
  dataLoaded: boolean = false;
  datePipe = inject(DatePipe);

  constructor(
    private detailedViewService: DetailedViewService,
    private sanitizer: DomSanitizer,
    private route: ActivatedRoute,
    private _snackBar: MatSnackBar,
    private queryToDetailedViewService: QueryToOtherPages
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe((params: Params) => {
      this.assetGuid = params['asset_guid'];
      this.initializeCurrentAsset(this.assetGuid);
    });
  }

  asset!: Asset;
  // TODO: For now: Barcodes only
  specimenBarcodes?: string | undefined;
  fileFormats?: string | undefined;
  restrictedAccess?: string | undefined;
  tags?: string | undefined;
  parentGuids: string = '';
  // TODO: For now: Event and Timestamp
  events?: string[] | undefined;

  thumbnailUrl?: SafeUrl;

  assetFiles?: string[] | undefined;
  displayedColumns: string[] = ['Files'];

  initializeCurrentAsset(assetGuid: string) {
    this.currentIndex = this.assetList.indexOf(this.assetGuid);

    this.fetchData(assetGuid);
  }

  fetchData(assetGuid: string) {
    // Steps: 1. Get Asset Metadata
    this.detailedViewService
      .getAssetMetadata(assetGuid)
      .pipe(
        take(1),
        switchMap((assetResponse) => {
          if (assetResponse) {
            this.asset = assetResponse;
            console.log(assetResponse);
            const specimen = (assetResponse?.asset_specimen ?? []).flatMap((a) => a?.specimen ?? []);
            this.specimenBarcodes = specimen.map((s) => s.barcode).join(', ');
            this.fileFormats = assetResponse?.file_formats?.map((file_format) => file_format).join(', ');
            this.restrictedAccess = assetResponse?.restricted_access?.map((type) => type).join(', ');
            this.parentGuids = assetResponse?.parent_guids?.join(', ') ?? '';
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
            return this.detailedViewService.getFileList(assetGuid).pipe(
              switchMap((fileList) => {
                if (fileList) {
                  this.assetFiles = fileList.map((filePath) => {
                    let parts: string[] = filePath.split('/');
                    return parts[parts.length - 1];
                  });
                  if (fileList.length > 0) {
                    return this.detailedViewService.getThumbnail(
                      assetResponse.institution ?? '',
                      assetResponse.collection ?? '',
                      assetGuid
                    );
                  }
                  return EMPTY;
                }
                return EMPTY;
              })
            );
          }
          return EMPTY;
        }),
        take(1)
      )
      .subscribe({
        next: (blob) => {
          if (blob) {
            const objectUrl = URL.createObjectURL(blob);
            this.thumbnailUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
          }
        },
        error: (err: Error) => console.log(err),
        complete: () => (this.dataLoaded = true)
      });
  }

  showNextAsset(): void {
    if (this.currentIndex < this.assetList.length - 1) {
      this.currentIndex++;
      this.assetGuid = this.assetList[this.currentIndex];
      this.fetchData(this.assetGuid);
    }
  }

  isNextDisabled(): boolean {
    return this.currentIndex === this.assetList.length - 1;
  }

  showPreviousAsset(): void {
    if (this.currentIndex > 0) {
      this.currentIndex--;
      this.assetGuid = this.assetList[this.currentIndex];
      this.fetchData(this.assetGuid);
    }
  }

  isPreviousDisabled(): boolean {
    return this.currentIndex === 0;
  }

  downloadCsv() {
    let currentAsset: string[] = [this.asset.asset_guid!];
    this.detailedViewService.postCsv(currentAsset).subscribe({
      next: (response) => {
        if (response.status === 200) {
          let guid: string = response.body;
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
                next: () => {},
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
    let currentAsset: string[] = [this.asset.asset_guid!];
    this.detailedViewService.postCsv(currentAsset).subscribe({
      next: (response) => {
        let guid: string = response.body;
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

  openSnackBar(message: string, action: string) {
    this._snackBar.open(message, action);
  }
}
