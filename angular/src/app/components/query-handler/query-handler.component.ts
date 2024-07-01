import {Component, EventEmitter, Input, OnInit, Output, ViewChild, ViewContainerRef} from '@angular/core';
import {QueryBuilderComponent} from "../query-builder/query-builder.component";
import {QueryView} from "../../types/query-types";

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
  @Input() first: boolean = false;
  @Input() idx: number | undefined;

  @Input()
  set nodes(nodes: Map<string, string[]>) {
    this.nodeMap = nodes;
  }

  get nodes() {
    return this.nodeMap;
  }

  constructor() { }

  ngOnInit(): void {
    console.log(Object.keys(localStorage))
    console.log(localStorage.getItem('forms-' + this.idx));
    const cachedWhere = localStorage.getItem('forms-' + this.idx);
    if (cachedWhere != null) {
      const wheres = new Map<string, string>(JSON.parse(cachedWhere))
      console.log(wheres)
      wheres.forEach((form, _idx) => {
        this.addWhere(form);
      })
    } else {
      this.addWhere(undefined);
    }
  }

  addWhere(form: string | undefined) {
    if (this.queryBuilderEle) {
      const newComponent = this.queryBuilderEle.createComponent(QueryBuilderComponent, {index: this.queryBuilderEle.length});
      newComponent.instance.nodes = this.nodes ? this.nodes : new Map;
      newComponent.instance.jsonForm = form;
      newComponent.instance.saveQueryEvent.subscribe(where => {
        this.saveQuery(where, this.queryBuilderEle!.indexOf(newComponent.hostView));
        this.formMap.set(this.queryBuilderEle!.indexOf(newComponent.hostView), JSON.stringify(newComponent.instance.queryForm.value));
        console.log(JSON.stringify(Array.from(this.formMap.entries())))
        localStorage.setItem('forms-' + this.idx, JSON.stringify(Array.from(this.formMap.entries())));
      });
      newComponent.instance.removeComponentEvent.subscribe(() => {
        this.removeQueryComponent(this.queryBuilderEle!.indexOf(newComponent.hostView));
        this.formMap.delete(this.queryBuilderEle!.indexOf(newComponent.hostView));
        localStorage.setItem('forms-' + this.idx, JSON.stringify(Array.from(this.formMap.entries())));
        newComponent.destroy();
      });
    }
  }

  removeQueryComponent(index: number) {
    this.queries.delete(index);
  }

  saveQuery(savedQuery: QueryView, index: number) {
    this.queries.set(index, savedQuery); // map to avoid duplicates if a value is updated
    this.saveQueryEvent.emit(Array.from(this.queries.values()).map(val => val));
  }

  removeComponent() {
    this.removeComponentEvent.emit();
  }
}
