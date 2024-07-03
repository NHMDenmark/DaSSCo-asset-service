import {Component, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {QueriesService} from "../../services/queries.service";
import {filter, iif, map, Observable, of, take} from "rxjs";
import {isNotUndefined} from "@northtech/ginnungagap";
import {Asset, Query, QueryView, QueryWhere, QueryResponse} from "../../types/query-types";
import {MatTableDataSource} from "@angular/material/table";
import {QueryHandlerComponent} from "../query-handler/query-handler.component";

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
  ) { }

  ngOnInit(): void {
    this.nodes$.pipe(filter(isNotUndefined),take(1))
      .subscribe(_nodes => this.newSelect())
  }

  newSelect() {
    if (this.queryHandlerEle) {
      const handlerComponent = this.queryHandlerEle.createComponent(QueryHandlerComponent, {index: this.queryHandlerEle.length});
      handlerComponent.instance.nodes = this.nodes;
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
    this.newSelect();
  }

  // clearcache() { // temp
  //   localStorage.clear();
  // }
}
