import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'dassco-illegal-asset-group-dialog',
  templateUrl: './illegal-asset-group-dialog.component.html',
  styleUrls: ['./illegal-asset-group-dialog.component.scss']
})
export class IllegalAssetGroupDialogComponent implements OnInit {

  constructor(public dialogRef: MatDialogRef<IllegalAssetGroupDialogComponent>
            , @Inject(MAT_DIALOG_DATA) public assets: string[]) { }

  ngOnInit(): void {
  }

  cancel() {
    this.dialogRef.close();
  }

  remove() {
     this.dialogRef.close(this.assets);
  }
}
