import {Component, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {QueriesService} from "../../services/queries.service";
import {filter, map, Observable, take} from "rxjs";
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

  nodes: Map<string, string[]> | undefined;
  queries: Map<number, QueryView[]> = new Map;

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
    this.nodes$.pipe(filter(isNotUndefined),take(1)).subscribe(() => this.newSelect()); // just adding the initial where. yet to find better way...
  }

  newSelect() {
    if (this.queryHandlerEle) {
      const newComponent = this.queryHandlerEle.createComponent(QueryHandlerComponent, {index: this.queryHandlerEle.length});
      newComponent.instance.nodes = this.nodes ? this.nodes : new Map;
      newComponent.instance.first = this.queryHandlerEle.length <= 1;
      newComponent.instance.saveQueryEvent.subscribe(queries => this.saveQuery(queries, this.queryHandlerEle!.indexOf(newComponent.hostView)));
      newComponent.instance.removeComponentEvent.subscribe(() => {
        this.removeQueryComponent(this.queryHandlerEle!.indexOf(newComponent.hostView));
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
      console.log(key)
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

    console.log(this.queries);
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
