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
      const builderComponent = this.queryBuilderEle.createComponent(QueryBuilderComponent, {index: this.queryBuilderEle.length});
      const eleIdx = this.queryBuilderEle!.indexOf(builderComponent.hostView);
      builderComponent.instance.nodes = this.nodeMap;
      if (form) { // this is dumb but the only reasonable way i found to "save" the cached forms. (for now)
        console.log(form)
        // this.cacheQuery(eleIdx, form);
        this.formMap.set(eleIdx, form);
      }
      builderComponent.instance.jsonForm = form;
      builderComponent.instance.saveQueryEvent.subscribe(where => {
        this.saveQuery(where, eleIdx);
        this.cacheForms(eleIdx, JSON.stringify(builderComponent.instance.queryForm.value));
      });
      builderComponent.instance.removeComponentEvent.subscribe(() => {
        this.removeQueryForm(eleIdx);
        this.formMap.delete(eleIdx);
        this.queryCacheService.setQueryForms(this.idx, this.formMap);
        builderComponent.destroy();
      });
    }
  }

  saveQuery(savedQuery: QueryView, index: number) {
    // console.log('HANDLER HERE', savedQuery)
    // console.log('HANDLER HERE', index)
    this.queries.set(index, savedQuery); // map to avoid duplicates if a value is updated. FROM BUILDER
    console.log(this.queries)
    this.saveQueryEvent.emit(Array.from(this.queries.values()).map(val => val));
  }

  cacheForms(eleIdx: number, jsonForm: string) {
    this.formMap.set(eleIdx, jsonForm);
    console.log('HANDLER CACHING', this.idx)
    console.log('HANDLER CACHING', this.formMap)
    this.queryCacheService.setQueryForms(this.idx, this.formMap);
  }

  removeQueryForm(index: number) {
    this.queries.delete(index);
  }

  removeComponent() {
    this.queryCacheService.removeQueryForm(this.idx);
    this.removeComponentEvent.emit();
  }
}
