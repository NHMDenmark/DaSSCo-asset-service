import { Component, OnInit } from '@angular/core';
import {DetailedViewService} from "../../services/detailed-view.service";
import {Asset} from "../../types";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {ActivatedRoute, Params} from "@angular/router";

@Component({
  selector: 'dassco-detailed-view',
  templateUrl: './detailed-view.component.html',
  styleUrls: ['./detailed-view.component.scss']
})
export class DetailedViewComponent implements OnInit {

  // Steps: 1. Get Asset Metadata
  // 2. Put the file names in a list. Check if there's an image with the substring "thumbnail"). If there is, send it to the service.
  // 3. Get Images. If Thumbnail, show thumbnail.
  // TODO: CHECK that the User has permission to view the asset.
  // TODO: Connection with Query page. The Query should pass the Asset[] from the search (in order!) so we can move back and forth between the assets.
  // The URL remains the same (on the first asset clicked) so on Back Button press we go back to the query list.
  // For this view to be seen you need to either have assets test-1, test-2 and test-3 in the DB or change the assetList for Assets you have.

  assetGuid: string = "";
  currentIndex : number = -1;
  // TODO: PLACEHOLDERS! ⬇ Change as soon as we have the connection to the Query page.
  assetList: string[] = ['test-1', 'test-2', 'test-3']
  dataLoaded: boolean = false;

  constructor(private detailedViewService: DetailedViewService, private sanitizer: DomSanitizer, private route: ActivatedRoute) { }

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
    this.detailedViewService.getAssetMetadata(assetGuid).subscribe((response) => {
      if (response){
        this.asset = response;
        this.convertToCsv();
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
        this.detailedViewService.getFileList(this.asset?.institution!, this.asset?.collection!, assetGuid).subscribe(response => {
          if (response){
            this.assetFiles = response;
            const thumbnail = this.assetFiles.find(file => file.includes('thumbnail') || "");
            if (thumbnail !== undefined){
              let lastSlashIndex = thumbnail.lastIndexOf("/");
              let fileName = thumbnail.substring(lastSlashIndex + 1);
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

  convertToCsv() {
    const separatorLine = 'sep=,\r\n';
    const headerRow = Object.keys(this.asset).join(",") + '\r\n';
    const dataRow = Object.values(this.asset).map(value => {
      if (value === null || value === undefined){
        return "null";
      }
      if (Array.isArray(value)){
        if (value.length !== 0){
          const formattedArray = value.map(item => JSON.stringify(item).replace(/"/g, '""')).join(', ');
          console.log(formattedArray)
          return `"${formattedArray}"`;
        } else {
          return "[]"
        }
      } else if (value instanceof Date) {
        return value.toISOString();
      } else if (value instanceof Object) {
        if (Object.entries(value).length !== 0){
          return Object.entries(value).map(([key, val]) => {
            return `${key}: ${val}`;
          })
        } else {
            return "{}"
        }
      } else {
        return value.toString();
      }
    }).join(',') + "\r\n";
    console.log(dataRow)
    return separatorLine + headerRow + dataRow;
  }

  exportCsv(){
    const csv = this.convertToCsv();
    const blob = new Blob([csv], { type: "text/csv; charset=utf-8" })
    const fileName = `${this.asset.asset_guid}.csv`;

    const link = document.createElement("a");
    if (link.download !== undefined){
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', fileName);
      link.style.visibility = "hidden";
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  }

}
