import {Component, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {QueriesService} from "../../services/queries.service";
import {filter, iif, map, Observable, of, take} from "rxjs";
import {isNotUndefined} from "@northtech/ginnungagap";
import {Asset, Query, QueryView, QueryWhere, QueryResponse} from "../../types/query-types";
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
  limit: number = this.queryCacheService.getLimit() ? this.queryCacheService.getLimit()! : 200;
  queries: Map<number, QueryView[]> = new Map;

  propertiesCall$: Observable<Map<string, string[]> | undefined>
    = this.queriesService.nodeProperties$
    .pipe(
      filter(isNotUndefined),
      map(nodes => {
        this.queryCacheService.setNodeProperties(nodes);
        return new Map(Object.entries(nodes));
      })
    )

  propertiesCached$
    = of (this.queryCacheService.getNodeProperties()).pipe(
    map(properties => properties)
  )

  nodes$: Observable<Map<string, string[]> | undefined>
  = iif(() => { // is this the "best" way of doing it? no clue. but it works. ¯\_(ツ)_/¯
      return this.queryCacheService.getNodeProperties().size == 0;
    },
    this.propertiesCall$, // if it's empty
    this.propertiesCached$ // if it's not empty
  );

  constructor(private queriesService: QueriesService
              , private queryCacheService: QueryCacheService
  ) { }

  ngOnInit(): void {
    // getting things setup once the nodes have loaded.
    this.nodes$.pipe(filter(isNotUndefined),take(1))
      .subscribe(_nodes => {
        const forms = this.queryCacheService.getCachedQueryKeys();
        if (forms.length > 0) { // there's data cached
          forms.forEach(() => this.newSelect())
          console.log('cached map', this.queryCacheService.getQueriesMap())
          this.queries = this.queryCacheService.getQueriesMap();
          this.dataSource.data = this.queryCacheService.getQueriesResults();
        } else {
          this.newSelect();
          }
      })
  }

  newSelect() {
    if (this.queryHandlerEle) {
      const handlerComponent = this.queryHandlerEle.createComponent(QueryHandlerComponent, {index: this.queryHandlerEle.length});
      handlerComponent.instance.nodes = this.queryCacheService.getNodeProperties();
      const childIdx = this.queryHandlerEle!.indexOf(handlerComponent.hostView);
      handlerComponent.instance.idx = childIdx;
      handlerComponent.instance.saveQueryEvent.subscribe(queries => this.saveQuery(queries, childIdx));
      handlerComponent.instance.removeComponentEvent.subscribe(() => {
        this.removeQueryComponent(childIdx);
        handlerComponent.destroy();
      });
    }
  }

  removeQueryComponent(index: number) {
    this.queries.delete(index);
    this.queryCacheService.setQueriesMap(this.queries);
  }

  saveQuery(queries: QueryView[], index: number) {
    console.log('from handler', queries)
    console.log('from handler', index)
    this.queries.set(index, queries);
    console.log('caching', JSON.stringify(Array.from(this.queries.entries())))
    this.queryCacheService.patchQueriesMapValue(index, queries);
    // this.queryCacheService.setQueriesMap(this.queries);
  }

  save() {
    const queryResponses: QueryResponse[] = [];

    this.queries.forEach((val, key) => {
      console.log(val)
      console.log(key)
      const nodeMap = new Map<string, QueryWhere[]>;
      val.forEach(where => {
        if (nodeMap.has(where.node)) {
          console.log('map has node ', where.node)
          nodeMap.get(where.node)!.push({property: where.property, fields: where.fields});
          console.log(nodeMap)
        } else {
          console.log('map does not have node, ', where.node)
          nodeMap.set(where.node, [{property: where.property, fields: where.fields}]);
          console.log(nodeMap)
        }
      })
      console.log('nodemap', nodeMap)
      const qv2s = Array.from(nodeMap).map((value) => {
        return {select: value[0], where: value[1]} as Query;
      })

      const response: QueryResponse = {id: key, query: qv2s};
      queryResponses.push(response);
    })

    console.log('saving queries', queryResponses)
    this.queriesService.getNodesFromQuery(queryResponses, this.limit).subscribe(result => {
      if (result) {
        this.queryCacheService.setLimit(this.limit);
        this.queryCacheService.setQueriesResults(result);
        this.dataSource.data = result;
      }
    })
  }

  clearAll() {
    this.queryHandlerEle?.clear();
    this.queries.clear();
    this.queryCacheService.clearData(true, true, true);
    this.newSelect();
  }

  clearcache() { // temp
    localStorage.clear();
  }
}
