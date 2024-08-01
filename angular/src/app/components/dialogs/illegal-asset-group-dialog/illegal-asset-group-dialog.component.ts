import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'dassco-illegal-asset-group-dialog',
  templateUrl: './illegal-asset-group-dialog.component.html',
  styleUrls: ['./illegal-asset-group-dialog.component.scss']
})
export class IllegalAssetGroupDialogComponent implements OnInit {
  assetList: string[] | undefined;

  constructor(public dialogRef: MatDialogRef<IllegalAssetGroupDialogComponent>
            , @Inject(MAT_DIALOG_DATA) public data: {assets: string | undefined, removable: boolean}) {
    if (data.assets) {
      this.assetList = data.assets.substring(1, data.assets.length-1).split(", ");
    }
  }

  ngOnInit(): void {
  }

  cancel() {
    this.dialogRef.close();
  }

  remove(returnData: string[] | undefined) {
     this.dialogRef.close(returnData);
  }
}
