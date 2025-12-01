import {Injectable} from '@angular/core';
import {QueryView} from '../types/query-types';
import {catchError, Observable, of, switchMap} from 'rxjs';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {HttpClient} from '@angular/common/http';
import {Digitiser} from '../types/types';
import {QueryItem} from '../types/queryItem';

@Injectable({
  providedIn: 'root'
})
export class CacheService {
  baseUrl = 'api/v1/caches';

  constructor(public oidcSecurityService: OidcSecurityService, private http: HttpClient) {}

  cachedDropdownValues$: Observable<Map<string, object[]> | undefined> = this.oidcSecurityService
    .getAccessToken()
    .pipe(
      switchMap((token) =>
        this.http
          .get<Map<string, object[]>>(`${this.baseUrl}`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(catchError(this.handleError(`get ${this.baseUrl}`, undefined)))
      )
    );

  cachedDigitisers$: Observable<Map<string, Digitiser[]> | undefined> = this.oidcSecurityService
    .getAccessToken()
    .pipe(
      switchMap((token) =>
        this.http
          .get<Map<string, Digitiser[]>>(`${this.baseUrl}/digitisers`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(catchError(this.handleError(`get ${this.baseUrl}/digitisers`, undefined)))
      )
    );

  setSetupTime() {
    const now = new Date().getTime();
    sessionStorage.setItem('setupTime', JSON.stringify(now));
  }

  checkSetupTime() {
    const now = new Date().getTime();
    const setupTime = sessionStorage.getItem('setupTime');
    if (setupTime == null) {
      sessionStorage.setItem('setupTime', JSON.stringify(now));
    } else {
      const cachedTime: number = JSON.parse(setupTime);
      if (now - cachedTime > 1 * 60 * 60 * 1000) {
        // 1 hr
        sessionStorage.clear();
        sessionStorage.setItem('setupTime', JSON.stringify(now));
      }
    }
  }

  setNodeProperties(properties: Map<string, string[]>) {
    sessionStorage.setItem('node-properties', JSON.stringify(properties));
    this.setSetupTime();
  }

  setQueryItems(queryItems: QueryItem[]) {
    sessionStorage.setItem('query-items', JSON.stringify(queryItems));
    this.setSetupTime();
  }

  getQueryItems(): QueryItem[] | undefined {
    const queryItems = sessionStorage.getItem('query-items');
    this.checkSetupTime();
    if (queryItems) {
      return JSON.parse(queryItems);
    }
    return undefined;
  }

  getNodeProperties(): Map<string, string[]> | undefined {
    const properties = sessionStorage.getItem('node-properties');
    this.checkSetupTime();
    if (properties) {
      return JSON.parse(properties);
    }
    return undefined;
  }

  setQueries(queries: {title: string | undefined; map: Map<string, QueryView[]>}) {
    const mapString = JSON.stringify(Object.fromEntries(queries.map));
    const newMap = {title: queries.title, map: mapString};
    sessionStorage.setItem('queries', JSON.stringify(newMap));
  }

  getQueries(): {title: string | undefined; map: Map<string, QueryView[]>} | undefined {
    this.checkSetupTime();
    let query: {title: string | undefined; map: Map<string, QueryView[]>} | undefined;
    const queries = sessionStorage.getItem('queries');
    if (queries) {
      const tempObj: {title: string | undefined; map: string} = JSON.parse(queries);
      query = {title: tempObj.title, map: new Map(Object.entries(JSON.parse(tempObj.map)))};
    }
    return query;
  }

  setQueryTitle(title: string | undefined) {
    const cached = this.getQueries();
    if (cached) {
      cached.title = title;
      this.setQueries(cached);
    }
  }

  setEnumValues(values: string[]) {
    sessionStorage.setItem('enum-values', JSON.stringify(values));
    this.setSetupTime();
  }

  getEnumValues(): string[] | undefined {
    const items = sessionStorage.getItem('enum-values');
    this.checkSetupTime();
    if (items) return JSON.parse(items);
    return undefined;
  }

  clearQueryCache() {
    sessionStorage.removeItem('queries');
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return of(result as T);
    };
  }
}
