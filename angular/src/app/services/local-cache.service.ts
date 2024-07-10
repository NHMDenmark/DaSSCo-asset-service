import { Injectable } from '@angular/core';
import {QueryView} from "../types/query-types";

@Injectable({
  providedIn: 'root'
})
export class LocalCacheService {

  constructor() { }

  setQueries(queries: Map<number, QueryView[]>) {
    localStorage.setItem('queries', JSON.stringify(Object.fromEntries(queries)));
  }

  getQueries(): Map<string, QueryView[]> | undefined {
    const queries = localStorage.getItem('queries');
    console.log(JSON.parse(queries!))
    return queries ? JSON.parse(queries) : undefined;
  }

  setQueryTitle(title: string) {
    localStorage.setItem('query-title', title);
  }

  getQueryTitle() {
    return localStorage.getItem('query-title');
  }
}
