import { Injectable } from '@angular/core';
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {catchError, Observable, of, switchMap} from "rxjs";
import {Asset, Query} from "../types";

@Injectable({
  providedIn: 'root'
})
export class QueriesService {
  baseUrl = '/api/v1/queries';

  constructor(public oidcSecurityService: OidcSecurityService
            , private http: HttpClient) { }

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

  getNodesFromQuery(query: Query[], limit: number): Observable<Asset[] | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<Asset[]>(`${this.baseUrl}/${limit}`, query, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/${limit}`, undefined))
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
