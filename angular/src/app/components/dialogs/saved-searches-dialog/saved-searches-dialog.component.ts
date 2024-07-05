import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {QueriesService} from "../../../services/queries.service";
import {QueryView, SavedQuery} from "../../../types/query-types";
import {map} from "rxjs";

@Component({
  selector: 'dassco-saved-searches-dialog',
  templateUrl: './saved-searches-dialog.component.html',
  styleUrls: ['./saved-searches-dialog.component.scss']
})
export class SavedSearchesDialogComponent implements OnInit {
  savedQueries$
    = this.queriesService.savedQueries$
    .pipe(
      map(saved => {
        console.log(saved)
        return saved;
      })
    )

  constructor(
    public dialogRef: MatDialogRef<SavedSearchesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
    private queriesService: QueriesService
  ) {}

  ngOnInit(): void {
  }

  cancel() {
    this.dialogRef.close();
  }

  chooseQuery(query: SavedQuery) {
    console.log(query)
    const queryMap: Map<string, QueryView[]> = new Map(Object.entries(JSON.parse(query.query)));
    console.log(queryMap)
    this.dialogRef.close({title: query.name, map: queryMap});
  }
}
