import {Component, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {QueriesService} from "../../services/queries.service";
import {filter, iif, map, Observable, of} from "rxjs";
import {isNotUndefined} from "@northtech/ginnungagap";
import {Asset, QueryV2, QueryView, QueryWhere, QueryResponse} from "../../types/query-types";
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
  cachedQueries: Map<number, Map<number, string>> = new Map;

  propertiesCall$: Observable<Map<string, string[]> | undefined>
    = this.queriesService.nodeProperties$
    .pipe(
      filter(isNotUndefined),
      map(nodes => {
        localStorage.setItem('nodeProperties', JSON.stringify(Object.entries(nodes)));
        return new Map(Object.entries(nodes));
      })
    )

  propertiesCached$
    = of (localStorage.getItem('nodeProperties')).pipe(
    map(properties => {
      if (properties != null) {
        return new Map<string, string[]>(JSON.parse(properties));
      }
      return undefined;
    })
  )

  nodes$: Observable<Map<string, string[]> | undefined>
  = iif(() => {
      return localStorage.getItem('nodeProperties') == null;
    },
    this.propertiesCall$, // if it's null
    this.propertiesCached$ // if it's not null
  );

  constructor(private queriesService: QueriesService
  ) { }

  ngOnInit(): void {
    const forms = Object.keys(localStorage).filter(key => key.includes('forms-'));
    console.log(forms.length)
    if (forms.length > 0) {
      forms.forEach(() => this.newSelect())
    } else {
      this.newSelect();
    }
    // this.nodes$.pipe(filter(isNotUndefined),take(1)).subscribe(() => this.newSelect()); // just adding the initial where. yet to find better way...
  }

  newSelect() {
    if (this.queryHandlerEle) {
      const newComponent = this.queryHandlerEle.createComponent(QueryHandlerComponent, {index: this.queryHandlerEle.length});
      newComponent.instance.nodes = localStorage.getItem('nodeProperties') != null ? new Map(JSON.parse(localStorage.getItem('nodeProperties')!)) : new Map()
      newComponent.instance.first = this.queryHandlerEle.length <= 1;
      newComponent.instance.idx = this.queryHandlerEle!.indexOf(newComponent.hostView);
      newComponent.instance.saveQueryEvent.subscribe(queries => {
        this.saveQuery(queries, this.queryHandlerEle!.indexOf(newComponent.hostView));
        this.cachedQueries.set(this.queryHandlerEle!.indexOf(newComponent.hostView), newComponent.instance.formMap);
        localStorage.setItem('views', JSON.stringify(Array.from(this.cachedQueries.entries())));
      });
      newComponent.instance.removeComponentEvent.subscribe(() => {
        this.removeQueryComponent(this.queryHandlerEle!.indexOf(newComponent.hostView));
        this.cachedQueries.delete(this.queryHandlerEle!.indexOf(newComponent.hostView));
        localStorage.setItem('views', JSON.stringify(Array.from(this.cachedQueries.entries())));
        newComponent.destroy();
      });
    }
  }

  removeQueryComponent(index: number) {
    this.queries.delete(index);
  }

  saveQuery(queries: QueryView[], index: number) {
    this.queries.set(index, queries);
    console.log(this.queries)
  }

  save() {
    // const fullMap = new Map<number, QueryV2[]>;
    const queryResponses: QueryResponse[] = [];

    this.queries.forEach((val, key) => {
      const nodeMap = new Map<string, QueryWhere[]>;
      val.forEach(where => {
        if (nodeMap.has(where.node)) {
          // add to the list
          nodeMap.get(where.node)!.push({property: where.property, fields: where.fields});
        } else {
          nodeMap.set(where.node, [{property: where.property, fields: where.fields}]);
        }
      })
      const qv2s = Array.from(nodeMap).map((value) => {
        return {select: value[0], where: value[1]} as QueryV2;
      })

      const test: QueryResponse = {id: key, query: qv2s};
      queryResponses.push(test);
      // fullMap.set(key, qv2s);
    })
    // console.log(fullMap)
    console.log(queryResponses)
    // const queries = Array.from(this.queries.values()).map(val => val);
    // console.log(queries)

    console.log('saving queries', queryResponses)
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
}
