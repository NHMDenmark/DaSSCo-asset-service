import { Injectable } from '@angular/core';
import {QueryView} from "../types/query-types";

@Injectable({
  providedIn: 'root'
})
export class LocalCacheService {

  constructor() { }

  setQueries(queries: {title: string | undefined, map: Map<string, QueryView[]>}) {
    const mapString = JSON.stringify(Object.fromEntries(queries.map));
    const newMap = {title: queries.title, map: mapString};
    localStorage.setItem('queries', JSON.stringify(newMap));
  }

  setQueryTitle(title: string | undefined) {
    const cached = this.getQueries();
    if (cached) {
      cached.title = title;
      this.setQueries(cached);
    }
  }

  getQueries(): {title: string | undefined, map: Map<string, QueryView[]>} | undefined {
    let query: {title: string | undefined, map: Map<string, QueryView[]>} | undefined;
    const queries = localStorage.getItem('queries');
    if (queries) {
      const tempObj: {title: string | undefined, map: string} = JSON.parse(queries);
      query = {title: tempObj.title, map: new Map(Object.entries(JSON.parse(tempObj.map)))};
    }
    return query;
  }

  clearQueryCache() {
    localStorage.removeItem('queries');
  }
}
