import { Component, OnInit } from '@angular/core';
import {DetailedViewService} from "../../services/detailed-view.service";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {ActivatedRoute, Params} from "@angular/router";
import {MatSnackBar} from '@angular/material/snack-bar';
import {Asset} from "../../types/types";

@Component({
  selector: 'dassco-detailed-view',
  templateUrl: './detailed-view.component.html',
  styleUrls: ['./detailed-view.component.scss']
})
export class DetailedViewComponent implements OnInit {
  // TODO: Connection with Query page. The Query should pass the Asset[] from the search (in order!) so we can move back and forth between the assets.
  // TODO: The connection with the query page has to include the creation of the method to save the Asset[] in the actual list here.
  // TODO: Files are now grabbed from the local machine. This is problematic, as an asset could be complete, therefore no local instance of the file would exist. We need to create an endpoint, get the API to call ERDA directly and get the file we want (for the thumbnail we can just get it directly, for the zip download we need to download it, zip it, send it, delete it).
  // Asset Guid is retrieved from the URL:
  assetGuid: string = "";
  currentIndex : number = -1;
  // TODO: PLACEHOLDERS! ⬇ Change as soon as we have the connection to the Query page.
  assetList: string[] = ['test-asset-1', 'test-asset-2', 'test-asset-3']
  dataLoaded: boolean = false;

  constructor(private detailedViewService: DetailedViewService, private sanitizer: DomSanitizer, private route: ActivatedRoute, private _snackBar: MatSnackBar) { }

  ngOnInit(): void {
    this.route.params.subscribe((params: Params) => {
      this.assetGuid = params['asset_guid'];
      this.initializeCurrentAsset(this.assetGuid);
    })
  }

  asset!: Asset;
  // TODO: For now: Barcodes only
  specimenBarcodes? : string | undefined;
  fileFormats? : string | undefined;
  restrictedAccess? : string | undefined;
  tags? : string | undefined;
  // TODO: For now: Event and Timestamp
  events? : string | undefined;

  thumbnailUrl? : SafeUrl;

  assetFiles? : string[] | undefined;
  displayedColumns: string[] = ['Files'];

  initializeCurrentAsset(assetGuid : string){
    this.currentIndex = this.assetList.indexOf(this.assetGuid);

    this.fetchData(assetGuid);
  }

  fetchData(assetGuid : string){
    // Steps: 1. Get Asset Metadata
    this.detailedViewService.getAssetMetadata(assetGuid).subscribe((response) => {
      if (response){
        this.asset = response;
        this.specimenBarcodes = this.asset?.specimens?.map(specimen => specimen.barcode).join(', ');
        this.fileFormats = this.asset?.file_formats?.map(file_format => file_format).join(', ');
        this.restrictedAccess = this.asset?.restricted_access?.map(type => type).join(", ");
        this.tags = Object.entries(this.asset?.tags ?? {})
          .map(([key, value]) => `${key}: ${value}`)
          .join(', ');
        this.events = this.asset?.events!.map(event => {
          return `Event: ${event.event}, Timestamp: ${event.timeStamp}`;
        }).join(", ");
        this.thumbnailUrl = "";
        // 2. Put the file names in a list. Check if there's an image with the substring "thumbnail"). If there is, send it to the service.
        this.detailedViewService.getFileList(this.asset?.institution!, this.asset?.collection!, assetGuid).subscribe(response => {
          if (response){
            this.assetFiles = response;
            const thumbnail = this.assetFiles.find(file => file.includes('thumbnail') || "");
            if (thumbnail !== undefined){
              let lastSlashIndex = thumbnail.lastIndexOf("/");
              let fileName = thumbnail.substring(lastSlashIndex + 1);
              // 3. Get Images. If Thumbnail, show thumbnail.
              this.detailedViewService.getThumbnail(this.asset?.institution!, this.asset?.collection!, assetGuid, fileName).subscribe(blob => {
                if (blob){
                  const objectUrl = URL.createObjectURL(blob);
                  this.thumbnailUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
                }
              });
            }
          }
        });
      }
      this.dataLoaded = true;
    });
  }

  showNextAsset(): void {
    if (this.currentIndex < this.assetList.length - 1){
      this.currentIndex ++;
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

  downloadCsv(){
    let currentAsset : string[] = [this.asset.asset_guid!];
    this.detailedViewService.postCsv(currentAsset)
      .subscribe({
        next: (response) => {
          if (response.status === 200){
            this.detailedViewService.getFile("assets.csv")
              .subscribe(
                {
                  next: (data) => {
                    const url = window.URL.createObjectURL(data);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = "assets.csv";

                    document.body.appendChild(link);
                    link.click();

                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);

                    this.detailedViewService.deleteFile(this.asset.asset_guid + ".csv", this.asset.institution, this.asset.collection, this.asset.asset_guid)
                      .subscribe({
                        next: () => {
                        },
                        error: () => {
                          this.openSnackBar("There's been an error deleting the CSV file", "Close")
                        }
                      })
                  },
                  error: () => {
                    this.openSnackBar("There has been an error downloading the CSV file.", "Close");
                  }
                })
          }
        },
        error: (error) => {
          this.openSnackBar(error.error, "Close");
        }
      });
    }

    downloadZip(){
      let currentAsset : string[] = [this.asset.asset_guid!];
      this.detailedViewService.postCsv(currentAsset)
        .subscribe({
          next: (response) => {
            if (response.status === 200){
              this.detailedViewService.postZip(this.asset.asset_guid + ".zip", this.asset.institution, this.asset.collection, this.asset.asset_guid)
                .subscribe({
                  next: (response) => {
                    if (response.status === 200){
                      this.detailedViewService.getFile(this.asset.asset_guid + ".zip")
                        .subscribe({
                          next: (data) => {
                            const url = window.URL.createObjectURL(data);
                            const link = document.createElement('a');
                            link.href = url;
                            link.download = this.asset.asset_guid + ".zip";

                            document.body.appendChild(link);
                            link.click();

                            document.body.removeChild(link);
                            window.URL.revokeObjectURL(url);

                            this.detailedViewService.deleteFile(this.asset.asset_guid + ".zip", this.asset.institution, this.asset.collection, this.asset.asset_guid)
                              .subscribe({
                                next: () => {
                                  this.detailedViewService.deleteFile(this.asset.asset_guid + ".csv", this.asset.institution, this.asset.collection, this.asset.asset_guid)
                                    .subscribe({
                                      next: () => {
                                      }, error: () => {
                                        this.openSnackBar("There has been an error deleting the CSV file", "Close");
                                      }
                                    })
                                },
                                error: () => {
                                  this.openSnackBar("There has been an error deleting the ZIP file", "Close");
                                }
                              })
                          },
                          error: () => {
                            this.openSnackBar("There was an error retrieving the file", "Close")
                          }
                        })
                    }
                  },
                  error: (error) => {
                    this.openSnackBar(error.error, "Close")
                  }
                })
            }
          },
          error: (error) => {
            this.openSnackBar(error.error, "Close")
          }
        })
    }

  openSnackBar(message: string, action: string) {
    this._snackBar.open(message, action);
  }
}
