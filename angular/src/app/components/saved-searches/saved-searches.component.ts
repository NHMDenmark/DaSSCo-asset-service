import {Component, OnInit} from '@angular/core';
import {MatDialog} from "@angular/material/dialog";
import {SavedSearchesDialogComponent} from "./dialog/saved-searches-dialog/saved-searches-dialog.component";

@Component({
  selector: 'dassco-saved-searches',
  templateUrl: './saved-searches.component.html',
  styleUrls: ['./saved-searches.component.scss']
})
export class SavedSearchesComponent implements OnInit {

  constructor(public dialog: MatDialog
  ) {}

  ngOnInit(): void {
  }
  saveSearch() {
    console.log('hi!')
  }

  openDialog(): void {
    const dialogRef = this.dialog.open(SavedSearchesDialogComponent, {
      width: '250px',
      data: 'hej!',
    });

    dialogRef.afterClosed().subscribe(result => {
      console.log('The dialog was closed');
      console.log(result)
    });
  }

}
