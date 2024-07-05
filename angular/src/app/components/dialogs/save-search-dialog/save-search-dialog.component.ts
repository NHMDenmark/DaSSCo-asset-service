import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'dassco-save-search-dialog',
  templateUrl: './save-search-dialog.component.html',
  styleUrls: ['./save-search-dialog.component.scss']
})
export class SaveSearchDialogComponent implements OnInit {
  title: string | undefined;

  constructor(
    public dialogRef: MatDialogRef<SaveSearchDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,) { }

  ngOnInit(): void {
  }

  cancel() {
    this.dialogRef.close();
  }

}
