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
      }
    });
  }

  asset?: Asset;

}
