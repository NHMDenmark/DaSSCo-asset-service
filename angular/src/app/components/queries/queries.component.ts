import {Component, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {QueriesService} from "../../services/queries.service";
import {filter, iif, map, Observable, of, take} from "rxjs";
import {isNotUndefined} from "@northtech/ginnungagap";
import {Asset, Query, QueryView, QueryWhere, QueryResponse} from "../../types/query-types";
import {MatTableDataSource} from "@angular/material/table";
import {QueryHandlerComponent} from "../query-handler/query-handler.component";
import {
  SavedSearchesDialogComponent
} from "../dialogs/saved-searches-dialog/saved-searches-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {SaveSearchDialogComponent} from "../dialogs/save-search-dialog/save-search-dialog.component";
import {MatSnackBar} from "@angular/material/snack-bar";

@Component({
  selector: 'dassco-queries',
  templateUrl: './queries.component.html',
  styleUrls: ['./queries.component.scss']
})
export class QueriesComponent implements OnInit {
  @ViewChild('queryHandlerContainer', { read: ViewContainerRef, static: true }) queryHandlerEle: ViewContainerRef | undefined;
  displayedColumns: string[] = ['asset_guid', 'status', 'multi_specimen', 'funding', 'subject', 'file_formats', 'internal_status',
    'tags', 'specimens', 'institution_name', 'collection_name', 'pipeline_name', 'workstation_name', 'creation_date', 'user_name'];
  dataSource = new MatTableDataSource<Asset>();
  limit: number = 200;
  queries: Map<number, QueryView[]> = new Map;
  nodes: Map<string, string[]> = new Map();
  queryTitle: string | undefined;
  queryUpdatedTitle: string | undefined;

  propertiesCall$: Observable<Map<string, string[]> | undefined>
    = this.queriesService.nodeProperties$
    .pipe(
      filter(isNotUndefined),
      map(nodes => {
        localStorage.setItem('node-properties', JSON.stringify(nodes));
        this.nodes = nodes;
        return new Map(Object.entries(nodes));
      })
    )

  propertiesCached$
    = of (localStorage.getItem('node-properties')).pipe(
    map(properties => {
      if (properties) {
        const propertiesMap = new Map<string, string[]>(JSON.parse(properties));
        this.nodes = propertiesMap;
        return propertiesMap;
      }
      return new Map();
    })
  )

  nodes$: Observable<Map<string, string[]> | undefined>
  = iif(() => { // is this the "best" way of doing it? no clue. but it works. ¯\_(ツ)_/¯
      return localStorage.getItem('node-properties') == null;
    },
    this.propertiesCall$, // if it's empty
    this.propertiesCached$ // if it's not empty
  );

  constructor(private queriesService: QueriesService
              , public dialog: MatDialog
              , private _snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.nodes$.pipe(filter(isNotUndefined),take(1))
      .subscribe(_nodes => this.newSelect(undefined))
  }

  newSelect(savedQuery: QueryView[] | undefined) {
    if (this.queryHandlerEle) {
      const handlerComponent = this.queryHandlerEle.createComponent(QueryHandlerComponent, {index: this.queryHandlerEle.length});
      handlerComponent.instance.nodes = this.nodes;
      handlerComponent.instance.savedQuery = savedQuery;
      const childIdx = this.queryHandlerEle!.indexOf(handlerComponent.hostView);
      handlerComponent.instance.idx = childIdx;
      handlerComponent.instance.saveQueryEvent.subscribe(queries => this.queries.set(childIdx, queries));
      handlerComponent.instance.removeComponentEvent.subscribe(() => {
        this.queries.delete(childIdx);
        handlerComponent.destroy();
      });
    }
  }

  saveQuery(queries: QueryView[], index: number) {
    this.queries.set(index, queries);
  }

  save() {
    const queryResponses: QueryResponse[] = [];

    this.queries.forEach((val, key) => {
      const nodeMap = new Map<string, QueryWhere[]>;
      val.forEach(where => {
        if (nodeMap.has(where.node)) {
          nodeMap.get(where.node)!.push({property: where.property, fields: where.fields});
        } else {
          nodeMap.set(where.node, [{property: where.property, fields: where.fields}]);
        }
      })
      const qv2s = Array.from(nodeMap).map((value) => {
        return {select: value[0], where: value[1]} as Query;
      })

      const response: QueryResponse = {id: key, query: qv2s};
      queryResponses.push(response);
    })

    this.queriesService.getNodesFromQuery(queryResponses, this.limit).subscribe(result => {
      if (result) {
        this.dataSource.data = result;
      }
    })
  }

  clearAll() {
    this.queryHandlerEle?.clear();
    this.queries.clear();
    this.dataSource.data = [];
    this.queryTitle = undefined;
    this.newSelect(undefined);
  }

  saveSearch() {
    const dialogRef = this.dialog.open(SaveSearchDialogComponent);

    dialogRef.afterClosed().subscribe((title: string | undefined) => {
      if (title) {
        this.queriesService.saveSearch({name: title, query: JSON.stringify(Object.fromEntries(this.queries))})
          .subscribe(saved => {
            if (saved) {
              this._snackBar.open('The search ' + title + ' has been saved.', 'OK');
            } else {
              this._snackBar.open('Error occurred when saving the search. Try again.', 'OK');
            }
          })
      }
    });
  }

  savedSearchesDialog(): void {
    const dialogRef = this.dialog.open(SavedSearchesDialogComponent, {
      width: '300px'
    });

    dialogRef.componentInstance.deleteQuery$
      .pipe(filter(isNotUndefined))
      .subscribe(queryName => {
        this.queriesService.deleteSavedSearch(queryName)
          .subscribe(deleted => {
            if (deleted) {
              this._snackBar.open('The search has been deleted.', 'OK');
            } else {
              this._snackBar.open('Error occurred. Try deleting again.', 'OK');
            }
          })
      });

    dialogRef.afterClosed().subscribe((queryMap: {title: string, map: Map<string, QueryView[]>} | undefined) => {
      if (queryMap) {
        this.queryTitle = queryMap.title;
        this.queryUpdatedTitle = queryMap.title;
        this.queryHandlerEle?.clear();
        Array.from(queryMap.map.keys()).forEach((key) => {
          this.newSelect(queryMap.map.get(key));
        });
      }
    });
  }

  updateSearch() {
    console.log(JSON.stringify(Object.fromEntries(this.queries)))
    if (this.queryUpdatedTitle && this.queryTitle) {
      this.queriesService.updateSavedSearch({name: this.queryUpdatedTitle, query: JSON.stringify(Object.fromEntries(this.queries))}, this.queryTitle)
        .subscribe(updated => {
          this.queryTitle = updated?.name;
          this.queryUpdatedTitle = updated?.name;
          console.log('updated', updated?.query)
        })
    }
  }

  // clearcache() { // temp
  //   localStorage.clear();
  // }
}
