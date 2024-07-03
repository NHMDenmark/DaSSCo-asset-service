import { Injectable } from '@angular/core';
import {Asset, QueryView} from "../types/query-types";

@Injectable({
  providedIn: 'root'
})
export class QueryCacheService {
  // just to avoid errors for accidental typos
  private readonly queriesResults: string = 'queries-results';
  private readonly queriesMap: string = 'queries-map';
  private readonly query: string = 'query-';
  private readonly nodeProperties: string = 'node-properties';
  private readonly limit: string = 'limit';

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

  setQueriesResults(queries: Asset[]) {
    localStorage.setItem(this.queriesResults, JSON.stringify(queries));
  }

  getQueriesResults(): Asset[] {
    const results = localStorage.getItem(this.queriesResults);
    if (results != null) return JSON.parse(results);
    return [];
  }

  clearQueriesResults() {
    localStorage.removeItem(this.queriesResults);
  }

  patchQueriesMapValue(index: number, queryView: QueryView[]) {
    const existing = localStorage.getItem(this.queriesMap);
    if (existing) {
      const queries = new Map<number, QueryView[]>(JSON.parse(existing));
      if (queries.has(index)) {
        const existingQueries = queries.get(index);
        if (existingQueries) {
          existingQueries.concat(queryView);
        } else {
          queries.set(index, queryView);
        }
      }
      this.setQueriesMap(queries);
    } else {
      this.setQueriesMap(new Map([[index, queryView]])); // nothing has been saved yet.
    }
  }

  setQueriesMapValue(index: number, queryView: QueryView[]) {
    const existing = localStorage.getItem(this.queriesMap);
    if (existing) {
      const queries = new Map<number, QueryView[]>(JSON.parse(existing));
      queries.set(index, queryView);
      this.setQueriesMap(queries);
    }
    return undefined;
  }

  getQueriesMapValue(index: number): QueryView[] | undefined {
    const existing = localStorage.getItem(this.queriesMap);
    if (existing) {
      const queries = new Map<number, QueryView[]>(JSON.parse(existing));
      if (queries.has(index)) return queries.get(index);
    }
    return undefined;
  }

  setQueriesMap(queries: Map<number, QueryView[]>) {
    localStorage.setItem(this.queriesMap, JSON.stringify(Array.from(queries.entries())));
  }

  getQueriesMap(): Map<number, QueryView[]> {
    const queries = localStorage.getItem(this.queriesMap);
    if (queries != null) {
      return new Map(JSON.parse(queries));
    }
    return new Map();
  }

  clearQueriesMap() {
    localStorage.removeItem(this.queriesMap);
  }

  getQueryForms(handlerIdx: number | undefined): Map<string, string> {
    const wheres = localStorage.getItem(this.query + handlerIdx);
    if (wheres != null) {
      return new Map<string, string>(JSON.parse(wheres))
    }
    return new Map();
  }

  setQueryForms(handlerIdx: number | undefined, queryMap: Map<number, string>) { // {0, query form as JSON}
    localStorage.setItem(this.query + handlerIdx, JSON.stringify(Array.from(queryMap.entries())));
  }

  removeQueryForm(handlerIdx: number | undefined) {
    localStorage.removeItem(this.query + handlerIdx);
  }

  setLimit(limit: number) {
    localStorage.setItem(this.limit, limit.toString());
  }

  getLimit(): number | undefined {
    const limit = localStorage.getItem(this.limit);
    if (limit) return parseInt(limit);
    return undefined;
  }

  removeLimit() {
    localStorage.removeItem(this.limit);
  }

  clearData(clearForms: boolean, clearQueries: boolean, clearQueryResult: boolean) {
    if (clearForms) {
      const forms = Object.keys(localStorage).filter(key => key.includes(this.query));
      forms.forEach(form => localStorage.removeItem(form));
    }
    if (clearQueries) {
      this.clearQueriesMap();
    }
    if (clearQueryResult) {
      this.clearQueriesResults();
    }
  }

  getCachedQueryKeys(): string[] {
    return Object.keys(localStorage).filter(key => key.includes(this.query));
  }
}
