import { Component, OnInit } from '@angular/core';
import {DetailedViewService} from "../../services/detailed-view.service";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {ActivatedRoute, Params} from "@angular/router";
import {MatSnackBar} from '@angular/material/snack-bar';
import {Asset} from "../../types/types";
import {QueryToOtherPages} from "../../services/query-to-other-pages";

@Component({
  selector: 'dassco-detailed-view',
  templateUrl: './detailed-view.component.html',
  styleUrls: ['./detailed-view.component.scss']
})
export class DetailedViewComponent implements OnInit {
  // Asset Guid is retrieved from the URL:
  assetGuid: string = "";
  currentIndex : number = -1;
  assetList: string[] = this.queryToDetailedViewService.getAssets();
  dataLoaded: boolean = false;

  constructor(private detailedViewService: DetailedViewService, private sanitizer: DomSanitizer,
              private route: ActivatedRoute, private _snackBar: MatSnackBar,
              private queryToDetailedViewService : QueryToOtherPages) { }

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
        this.detailedViewService.getFileList(assetGuid).subscribe(response => {
          if (response){
            this.assetFiles = response.map(filePath =>{
              let parts : string[] = filePath.split('/');
              return parts[parts.length - 1];
            })
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

                    this.detailedViewService.deleteFile()
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
              this.detailedViewService.postZip(currentAsset)
                .subscribe({
                  next: (response) => {
                    if (response.status === 200){
                      this.detailedViewService.getFile("assets.zip")
                        .subscribe({
                          next: (data) => {
                            const url = window.URL.createObjectURL(data);
                            const link = document.createElement('a');
                            link.href = url;
                            link.download = "assets.zip";

                            document.body.appendChild(link);
                            link.click();

                            document.body.removeChild(link);
                            window.URL.revokeObjectURL(url);

                            this.detailedViewService.deleteFile()
                              .subscribe({
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
