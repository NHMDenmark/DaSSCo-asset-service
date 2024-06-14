import { Component, OnInit } from '@angular/core';
import {DetailedViewService} from "../../services/detailed-view.service";
import {Asset} from "../../types";

@Component({
  selector: 'dassco-detailed-view',
  templateUrl: './detailed-view.component.html',
  styleUrls: ['./detailed-view.component.scss']
})
export class DetailedViewComponent implements OnInit {

  // TODO:
  // Steps: 1. Get Asset Metadata
  // 2. Get Images. If Thumbnail, show thumbnail.
  // 3. Put the files in a list: In the future the option to download them will be there!

  constructor(private detailedViewService: DetailedViewService) { }

  ngOnInit(): void {
    this.detailedViewService.getAssetMetadata().subscribe((response) => {
      if (response){
        console.log(response)
        this.asset = response;
        this.specimenBarcodes = this.asset?.specimens?.map(specimen => specimen.barcode).join(', ');
        this.fileFormats = this.asset?.file_formats?.map(file_format => file_format).join(', ');
        this.restrictedAccess = this.asset?.restricted_access?.map(type => type).join(", ");
        this.tags = Object.entries(this.asset?.tags ?? {})
            .map(([key, value]) => `${key}: ${value}`)
            .join(', ');
        this.events = this.asset?.events!.map(event => {
          return `Event: ${event.event}, Timestamp: ${event.timeStamp}`;
        }).join("\n");
      }
    });
  }

  asset?: Asset;
  // TODO: For now: Barcodes only
  specimenBarcodes? : string | undefined;
  fileFormats? : string | undefined;
  restrictedAccess? : string | undefined;
  tags? : string | undefined;
  events? : string | undefined;
}
