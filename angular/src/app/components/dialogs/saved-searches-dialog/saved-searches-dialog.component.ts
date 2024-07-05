import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {QueriesService} from "../../../services/queries.service";
import {QueryView, SavedQuery} from "../../../types/query-types";
import {BehaviorSubject, filter, map} from "rxjs";
import {isNotUndefined} from "@northtech/ginnungagap";

@Component({
  selector: 'dassco-saved-searches-dialog',
  templateUrl: './saved-searches-dialog.component.html',
  styleUrls: ['./saved-searches-dialog.component.scss']
})
export class SavedSearchesDialogComponent implements OnInit {
  deleteQuerySubject = new BehaviorSubject<string | undefined>(undefined);
  deleteQuery$ = this.deleteQuerySubject.asObservable();
  savedQueries: SavedQuery[] = [];

  savedQueries$
    = this.queriesService.savedQueries$
    .pipe(
      filter(isNotUndefined),
      map(saved => {
        this.savedQueries = saved;
        return saved;
      })
    )

  constructor(
    public dialogRef: MatDialogRef<SavedSearchesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {title: string, queryMap: Map<string, QueryView[]>},
    private queriesService: QueriesService
  ) {}

  ngOnInit(): void {
  }

  cancel() {
    this.dialogRef.close();
  }

  deleteSavedSearch(query: SavedQuery) {
    this.savedQueries = this.savedQueries.filter(sq => sq.name != query.name);
    this.deleteQuerySubject.next(query.name);
  }

  chooseQuery(query: SavedQuery) {
    const queryMap: Map<string, QueryView[]> = new Map(Object.entries(JSON.parse(query.query)));
    this.dialogRef.close({title: query.name, map: queryMap});
  }
}
