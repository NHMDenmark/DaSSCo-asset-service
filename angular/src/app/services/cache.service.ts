import { Injectable } from '@angular/core';
import {QueryView} from "../types/query-types";
import {catchError, Observable, of, switchMap} from "rxjs";
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";

@Injectable({
  providedIn: 'root'
})
export class CacheService {
  baseUrl = '/api/v1/caches';

  constructor(public oidcSecurityService: OidcSecurityService
    , private http: HttpClient) { }

  cachedDropdownValues$: Observable<Map<string, object[]> | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get<Map<string, object[]>>(`${this.baseUrl}`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}`, undefined))
          );
      })
    );

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

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return of(result as T);
    };
  }
}
