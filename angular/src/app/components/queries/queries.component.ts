import {Component, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {QueriesService} from "../../services/queries.service";
import {filter, map, Observable} from "rxjs";
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
  displayedColumns: string[] = ['asset_guid', 'status', 'multi_specimen', 'funding', 'subject', 'file_formats', 'asset_taken_date',
    'internal_status', 'tags', 'specimens', 'institution_name', 'collection_name', 'pipeline_name', 'workstation_name', 'creation_date', 'user_name'];
  dataSource = new MatTableDataSource<Asset>();
  limit: number = 200;
  nodes: Map<string, string[]> | undefined;
  queries: Query[] = [];

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
  }

  addWhere() {
    console.log(this.queryBuilderEle)
    if (this.queryBuilderEle) {
      const newComponent = this.queryBuilderEle.createComponent(QueryBuilderComponent);
      // console.log(this.nodes)
      newComponent.instance.nodes = this.nodes ? this.nodes : new Map;
      newComponent.instance.saveQueryEvent.subscribe(queryFields => this.saveQuery(queryFields));
      newComponent.instance.removeComponentEvent.subscribe(() => newComponent.destroy());
    }
  }

  saveQuery(savedQuery: Query) {
    // console.log(savedQuery)
    this.queries.push(savedQuery)
  }

  save() {
    console.log('saving queries', this.queries)
    // console.log(this.limit)
    this.queriesService.getNodesFromQuery(this.queries, this.limit).subscribe(result => {
      // console.log(result)
      if (result) {
        this.dataSource.data = result;
      }
    })
  }
}
