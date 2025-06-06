import {Component, EventEmitter, Input, OnInit, Output, ViewChild, ViewContainerRef} from '@angular/core';
import {QueryBuilderComponent} from '../query-builder/query-builder.component';
import {QueryView} from '../../types/query-types';

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
  @Input() idx: number | undefined;
  @Input() savedQuery: QueryView[] | undefined;

  @Input()
  set nodes(nodes: Map<string, string[]>) {
    this.nodeMap = nodes;
  }

  ngOnInit(): void {
    if (this.savedQuery) {
      this.savedQuery.forEach(query => {
        this.addWhere(query);
      });
    } else {
      this.addWhere(undefined);
    }
  }

  addWhere(savedQuery: QueryView | undefined) {
    if (this.queryBuilderEle) {
      const builderComponent = this.queryBuilderEle.createComponent(QueryBuilderComponent, {index: this.queryBuilderEle.length});
      const eleIdx = this.queryBuilderEle!.indexOf(builderComponent.hostView);
      builderComponent.instance.nodes = this.nodeMap;
      builderComponent.instance.savedQuery = savedQuery;
      builderComponent.instance.saveQueryEvent.subscribe(where => {
        this.saveQuery(where, eleIdx);
      });
      builderComponent.instance.removeComponentEvent.subscribe(() => {
        this.removeQueryForm(eleIdx);
        builderComponent.destroy();
      });
    }
  }

  saveQuery(savedQuery: QueryView, index: number) {
    this.queries.set(index, savedQuery); // map to avoid duplicates if a value is updated. FROM BUILDER
    this.saveQueryEvent.emit(Array.from(this.queries.values()).map(val => val));
  }

  removeQueryForm(index: number) {
    this.queries.delete(index);
    this.saveQueryEvent.emit(Array.from(this.queries.values()).map(val => val));
  }

  removeComponent() {
    this.removeComponentEvent.emit();
  }
}
