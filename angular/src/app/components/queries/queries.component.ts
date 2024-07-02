import {Component, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {QueriesService} from "../../services/queries.service";
import {filter, iif, map, Observable, of, take} from "rxjs";
import {isNotUndefined} from "@northtech/ginnungagap";
import {Asset, QueryV2, QueryView, QueryWhere, QueryResponse} from "../../types/query-types";
import {MatTableDataSource} from "@angular/material/table";
import {QueryHandlerComponent} from "../query-handler/query-handler.component";
import {QueryCacheService} from "../../services/query-cache.service";

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

  propertiesCall$: Observable<Map<string, string[]> | undefined>
    = this.queriesService.nodeProperties$
    .pipe(
      filter(isNotUndefined),
      map(nodes => {
        this.queryCacheService.setNodeProperties(nodes);
        console.log('from service', new Map(Object.entries(nodes)))
        return new Map(Object.entries(nodes));
      })
    )

  propertiesCached$
    = of (this.queryCacheService.getNodeProperties()).pipe(
    map(properties => properties)
  )

  nodes$: Observable<Map<string, string[]> | undefined>
  = iif(() => {
      return this.queryCacheService.getNodeProperties().size == 0;
    },
    this.propertiesCall$, // if it's empty
    this.propertiesCached$ // if it's not empty
  );

  constructor(private queriesService: QueriesService
              , private queryCacheService: QueryCacheService
  ) { }

  ngOnInit(): void {
    this.nodes$.pipe(filter(isNotUndefined),take(1)).subscribe(_nodes => {
      const forms = this.queryCacheService.getFormKeys();
      if (forms.length > 0) {
        forms.forEach(() => this.newSelect())
        const cachedQueries = this.queryCacheService.getQueriesData();
        this.queries = cachedQueries ? cachedQueries : new Map();
      } else {
        this.newSelect();
      }
    })
  }

  newSelect() {
    if (this.queryHandlerEle) {
      const newComponent = this.queryHandlerEle.createComponent(QueryHandlerComponent, {index: this.queryHandlerEle.length});
      newComponent.instance.nodes = this.queryCacheService.getNodeProperties();
      newComponent.instance.idx = this.queryHandlerEle!.indexOf(newComponent.hostView);
      newComponent.instance.saveQueryEvent.subscribe(queries => this.saveQuery(queries, this.queryHandlerEle!.indexOf(newComponent.hostView)));
      newComponent.instance.removeComponentEvent.subscribe(() => {
        this.removeQueryComponent(this.queryHandlerEle!.indexOf(newComponent.hostView));
        newComponent.destroy();
      });
    }
  }

  removeQueryComponent(index: number) {
    this.queries.delete(index);
    this.queryCacheService.setQueriesData(this.queries);
  }

  saveQuery(queries: QueryView[], index: number) {
    this.queries.set(index, queries);
    console.log('caching', JSON.stringify(Array.from(this.queries.entries())))
    this.queryCacheService.setQueriesData(this.queries);
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
        return {select: value[0], where: value[1]} as QueryV2;
      })

      const response: QueryResponse = {id: key, query: qv2s};
      queryResponses.push(response);
    })

    console.log('saving queries', queryResponses)
    this.queriesService.getNodesFromQuery(queryResponses, this.limit).subscribe(result => {
      if (result) this.dataSource.data = result;
    })
  }

  clearAll() {
    this.queryHandlerEle?.clear();
    this.queries.clear();
    this.queryCacheService.clearData(true, true);
    this.newSelect();
  }

  clearcache() { // temp
    localStorage.clear();
  }
}
