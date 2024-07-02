import {Component, EventEmitter, Input, OnInit, Output, ViewChild, ViewContainerRef} from '@angular/core';
import {QueryBuilderComponent} from "../query-builder/query-builder.component";
import {QueryView} from "../../types/query-types";
import {QueryCacheService} from "../../services/query-cache.service";

@Component({
  selector: 'dassco-query-handler',
  templateUrl: './query-handler.component.html',
  styleUrls: ['./query-handler.component.scss']
})
export class QueryHandlerComponent implements OnInit {
  @ViewChild('queryBuilderContainer', { read: ViewContainerRef, static: true }) queryBuilderEle: ViewContainerRef | undefined;
  queries: Map<number, QueryView> = new Map;
  nodeMap: Map<string, string[]> = new Map<string, string[]>();
  formMap: Map<number, string> = new Map();
  @Output() removeComponentEvent = new EventEmitter<any>();
  @Output() saveQueryEvent = new EventEmitter<QueryView[]>();
  @Input() idx: number | undefined;

  @Input()
  set nodes(nodes: Map<string, string[]>) {
    this.nodeMap = nodes;
  }

  constructor(private queryCacheService: QueryCacheService) { }

  ngOnInit(): void {
    let cachedWhere = this.queryCacheService.getQueryForms(this.idx);
    if (cachedWhere != null && cachedWhere.size != 0) {
      cachedWhere.forEach((form, _idx) => this.addWhere(form))
    } else {
      this.addWhere(undefined);
    }
  }

  addWhere(form: string | undefined) {
    if (this.queryBuilderEle) {
      const newComponent = this.queryBuilderEle.createComponent(QueryBuilderComponent, {index: this.queryBuilderEle.length});
      const eleIdx = this.queryBuilderEle!.indexOf(newComponent.hostView);
      newComponent.instance.nodes = this.nodeMap;
      if (form) { // this is dumb but the only reasonable way i found to "save" the cached forms. (for now)
        this.cacheQuery(eleIdx, form);
      }
      newComponent.instance.jsonForm = form;
      newComponent.instance.saveQueryEvent.subscribe(where => {
        this.saveQuery(where, eleIdx);
        this.cacheQuery(eleIdx, JSON.stringify(newComponent.instance.queryForm.value));
      });
      newComponent.instance.removeComponentEvent.subscribe(() => {
        console.log(eleIdx)
        this.removeQueryComponent(eleIdx);
        this.formMap.delete(eleIdx);
        this.queryCacheService.setQueryForms(this.idx, this.formMap);
        newComponent.destroy();
      });
    }
  }

  cacheQuery(eleIdx: number, jsonForm: string) {
    this.formMap.set(eleIdx, jsonForm);
    this.queryCacheService.setQueryForms(this.idx, this.formMap);
  }

  removeQueryComponent(index: number) {
    this.queries.delete(index);
  }

  saveQuery(savedQuery: QueryView, index: number) {
    this.queries.set(index, savedQuery); // map to avoid duplicates if a value is updated
    this.saveQueryEvent.emit(Array.from(this.queries.values()).map(val => val));
  }

  removeComponent() {
    console.log(this.idx)
    this.queryCacheService.removeQueryForm(this.idx);
    this.removeComponentEvent.emit();
  }
}
