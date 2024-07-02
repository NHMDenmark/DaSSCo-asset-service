import { Injectable } from '@angular/core';
import {QueryView} from "../types/query-types";

@Injectable({
  providedIn: 'root'
})
export class QueryCacheService {
  private readonly queries: string = 'queries'; // just to avoid errors for accidental typos
  private readonly query: string = 'query-';
  private readonly nodeProperties: string = 'nodeProperties';

  constructor() { }

  getNodeProperties(): Map<string, string[]> {
    const properties = localStorage.getItem(this.nodeProperties);
    if (properties != null) {
      return new Map<string, string[]>(JSON.parse(properties));
    }
    return new Map();
  }

  setNodeProperties(nodeProps: Map<string, string[]>) {
    localStorage.setItem(this.nodeProperties, JSON.stringify(Object.entries(nodeProps)));
  }

  clearNodeProperties() {
    localStorage.removeItem(this.nodeProperties);
  }

  getQueriesData(): Map<number, QueryView[]> {
    const queries = localStorage.getItem(this.queries);
    if (queries != null) {
      return new Map(JSON.parse(queries));
    }
    return new Map();
  }

  setQueriesData(queries: Map<number, QueryView[]>) {
    localStorage.setItem(this.queries, JSON.stringify(Array.from(queries.entries())));
  }

  clearQueriesData() {
    localStorage.removeItem(this.queries);
  }

  getQueryForms(handlerIdx: number | undefined): Map<string, string> {
    const wheres = localStorage.getItem(this.query + handlerIdx);
    if (wheres != null) {
      return new Map<string, string>(JSON.parse(wheres))
    }
    return new Map();
  }

  setQueryForms(handlerIdx: number | undefined, queryMap: Map<number, string>) {
    localStorage.setItem(this.query + handlerIdx, JSON.stringify(Array.from(queryMap.entries())));
  }

  removeQueryForm(handlerIdx: number | undefined) {
    localStorage.removeItem(this.query + handlerIdx);
  }

  clearData(clearForms: boolean, clearQueries: boolean) {
    if (clearForms) {
      const forms = Object.keys(localStorage).filter(key => key.includes(this.query));
      forms.forEach(form => localStorage.removeItem(form));
    }
    if (clearQueries) {
      this.clearQueriesData();
    }
  }

  getFormKeys(): string[] {
    return Object.keys(localStorage).filter(key => key.includes(this.query));
  }
}
