import {Component, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {QueriesService} from "../../services/queries.service";
import {filter, map, Observable, take} from "rxjs";
import {isNotUndefined} from "@northtech/ginnungagap";
import {QueryBuilderComponent} from "../query-builder/query-builder.component";
import {Asset, Query} from "../../types";
import {MatTableDataSource} from "@angular/material/table";

@Component({
  selector: 'dassco-queries',
  templateUrl: './queries.component.html',
  styleUrls: ['./queries.component.scss']
})
export class QueriesComponent implements OnInit {
  @ViewChild('queryBuilderContainer', { read: ViewContainerRef, static: true }) queryBuilderEle: ViewContainerRef | undefined;
  displayedColumns: string[] = ['asset_guid', 'status', 'multi_specimen', 'funding', 'subject', 'file_formats', 'internal_status',
    'tags', 'specimens', 'institution_name', 'collection_name', 'pipeline_name', 'workstation_name', 'creation_date', 'user_name'];
  dataSource = new MatTableDataSource<Asset>();
  limit: number = 200;
  nodes: Map<string, string[]> | undefined;
  queries: Map<number, Query> = new Map;

  nodes$: Observable<Map<string, string[]> | undefined>
    = this.queriesService.nodeProperties$
    .pipe(
      filter(isNotUndefined),
      map(nodes => {
        this.nodes = new Map(Object.entries(nodes));
        return new Map(Object.entries(nodes));
      })
    )

  constructor(private queriesService: QueriesService
  ) { }

  ngOnInit(): void {
    this.nodes$.pipe(filter(isNotUndefined),take(1)).subscribe(() => this.addWhere()); // just adding the initial where
  }

  addWhere() {
    if (this.queryBuilderEle) {
      const newComponent = this.queryBuilderEle.createComponent(QueryBuilderComponent, {index: this.queryBuilderEle.length});
      newComponent.instance.nodes = this.nodes ? this.nodes : new Map;
      newComponent.instance.saveQueryEvent.subscribe(queryFields => this.saveQuery(queryFields, this.queryBuilderEle!.indexOf(newComponent.hostView)));
      newComponent.instance.removeComponentEvent.subscribe(() => {
        this.removeQueryComponent(this.queryBuilderEle!.indexOf(newComponent.hostView));
        newComponent.destroy();
      });
    }
  }

  removeQueryComponent(index: number) {
    this.queries.delete(index);
  }

  saveQuery(savedQuery: Query, index: number) {
    this.queries.set(index, savedQuery); // map to avoid duplicates if a value is updated
  }

  save() {
    const queries = Array.from(this.queries.values()).map(val => val);
    console.log('saving queries', queries)
    this.queriesService.getNodesFromQuery(queries, this.limit).subscribe(result => {
      if (result) {
        this.dataSource.data = result;
      }
    })
  }
}
