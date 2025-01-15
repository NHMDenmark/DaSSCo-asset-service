import { Injectable } from '@angular/core';
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {catchError, Observable, of, switchMap} from "rxjs";
import {QueryResponse, SavedQuery} from "../types/query-types";
import {Asset} from "../types/types";

@Injectable({
  providedIn: 'root'
})
export class QueriesService {
  baseUrl = 'api/v1/queries';

  constructor(public oidcSecurityService: OidcSecurityService
            , private http: HttpClient) {}

  nodeProperties$
    = this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get<Map<string, string[]>>(`${this.baseUrl}/nodes`, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/nodes`, undefined))
            );
        })
      );

  savedQueries$
    = this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get<SavedQuery[]>(`${this.baseUrl}/saved`, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/nodes`, undefined))
            );
        })
      );

  saveSearch(savedQuery: SavedQuery): Observable<SavedQuery | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<SavedQuery>(`${this.baseUrl}/save`, JSON.stringify(savedQuery), {headers: {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json; charset=utf-8'}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/save`, undefined))
            );
        })
      );
  }

  updateSavedSearch(savedQuery: SavedQuery, title: string): Observable<SavedQuery | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<SavedQuery>(`${this.baseUrl}/saved/update/${title}`, JSON.stringify(savedQuery), {headers: {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json; charset=utf-8'}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/save`, undefined))
            );
        })
      );
  }

  getAssetsFromQuery(queries: QueryResponse[], limit: number): Observable<Asset[] | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<Asset[]>(`${this.baseUrl}/${limit}`, JSON.stringify(queries), {headers: {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json; charset=utf-8'}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/${limit}`, undefined))
            );
        })
      );
  }

  getAssetCountFromQuery(queries: QueryResponse[], limit: number): Observable<number | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<number>(`${this.baseUrl}/assetcount/${limit}`, JSON.stringify(queries), {headers: {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json; charset=utf-8'}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/assetcount${limit}`, undefined))
            );
        })
      );
  }

  deleteSavedSearch(title: string): Observable<string | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.delete<string>(`${this.baseUrl}/saved/${title}`, {headers: {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json; charset=utf-8'}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/${title}`, undefined))
            );
        })
      );
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return of(result as T);
    };
  }
}
