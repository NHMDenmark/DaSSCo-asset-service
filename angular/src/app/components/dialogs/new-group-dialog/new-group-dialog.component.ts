import { Component, OnInit } from '@angular/core';
import {MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'dassco-new-group-dialog',
  templateUrl: './new-group-dialog.component.html',
  styleUrls: ['./new-group-dialog.component.scss']
})
export class NewGroupDialogComponent implements OnInit {

  constructor(public dialogRef: MatDialogRef<NewGroupDialogComponent>) { }

  ngOnInit(): void {
  }

  cancel() {
    this.dialogRef.close();
  }

  save() {

  }

}
