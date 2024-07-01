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
  @Output() removeComponentEvent = new EventEmitter<any>();
  @Output() saveQueryEvent = new EventEmitter<QueryView[]>();
  @Input() first: boolean = false;

  @Input()
  set nodes(nodes: Map<string, string[]>) {
    this.nodeMap = nodes;
    this.addWhere();
  }

  get nodes() {
    return this.nodeMap;
  }

  constructor() { }

  ngOnInit(): void {
  }

  addWhere() {
    if (this.queryBuilderEle) {
      const newComponent = this.queryBuilderEle.createComponent(QueryBuilderComponent, {index: this.queryBuilderEle.length});
      newComponent.instance.nodes = this.nodes ? this.nodes : new Map;
      newComponent.instance.saveQueryEvent.subscribe(where => this.saveQuery(where, this.queryBuilderEle!.indexOf(newComponent.hostView)));
      newComponent.instance.removeComponentEvent.subscribe(() => {
        this.removeQueryComponent(this.queryBuilderEle!.indexOf(newComponent.hostView));
        newComponent.destroy();
      });
    }
  }

  removeQueryComponent(index: number) {
    this.queries.delete(index);
  }

  saveQuery(savedQuery: QueryView, index: number) {
    this.queries.set(index, savedQuery); // map to avoid duplicates if a value is updated

    console.log(this.queries)

    this.saveQueryEvent.emit(Array.from(this.queries.values()).map(val => val));
  }

  removeComponent() {
    this.removeComponentEvent.emit();
  }
}
