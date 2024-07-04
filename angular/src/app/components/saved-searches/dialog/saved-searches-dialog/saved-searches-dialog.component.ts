import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'dassco-saved-searches-dialog',
  templateUrl: './saved-searches-dialog.component.html',
  styleUrls: ['./saved-searches-dialog.component.scss']
})
export class SavedSearchesDialogComponent implements OnInit {

  constructor(
    public dialogRef: MatDialogRef<SavedSearchesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
  ) {}

  onNoClick(): void {
    this.dialogRef.close();
  }

  ngOnInit(): void {
  }

  saveSearch() {
    console.log('hi!')
  }
}
