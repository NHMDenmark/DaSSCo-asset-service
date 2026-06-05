import {Injectable} from '@angular/core';
import {QueryView} from '../types/query-types';
import {catchError, Observable, of, switchMap} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {Digitiser} from '../types/types';
import {QueryItem} from '../types/queryItem';
import {AuthService} from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class CacheService {
  baseUrl = 'api/v1/caches';
  private readonly cacheKeys = ['setupTime', 'node-properties', 'query-items', 'queries', 'enum-values'];

  constructor(private authService: AuthService, private http: HttpClient) {}

  cachedDropdownValues$: Observable<Map<string, object[]> | undefined> = this.authService
    .getAccessToken()
    .pipe(
      switchMap((token) =>
        this.http
          .get<Map<string, object[]>>(`${this.baseUrl}`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(catchError(this.handleError(`get ${this.baseUrl}`, undefined)))
      )
    );

  cachedDigitisers$: Observable<Map<string, Digitiser[]> | undefined> = this.authService
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
        this.clearCachedSessionState();
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
    this.checkSetupTime();
    const queryItems = sessionStorage.getItem('query-items');
    if (queryItems) {
      return JSON.parse(queryItems);
    }
    return undefined;
  }

  getNodeProperties(): Map<string, string[]> | undefined {
    this.checkSetupTime();
    const properties = sessionStorage.getItem('node-properties');
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
    this.checkSetupTime();
    const items = sessionStorage.getItem('enum-values');
    if (items) return JSON.parse(items);
    return undefined;
  }

  clearQueryCache() {
    sessionStorage.removeItem('queries');
  }

  private clearCachedSessionState() {
    this.cacheKeys.forEach((key) => sessionStorage.removeItem(key));
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return of(result as T);
    };
  }
}
