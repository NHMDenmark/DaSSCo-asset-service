import { Injectable } from '@angular/core';
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient, HttpResponse} from "@angular/common/http";
import {catchError, Observable, of, switchMap} from "rxjs";
import {SpecimenGraph} from "../types";

@Injectable({
  providedIn: 'root'
})
export class SpecimenGraphService {
  baseUrl = '/api/v1/graphdata';

  constructor(
    public oidcSecurityService: OidcSecurityService
    , private http: HttpClient
  ) { }

  specimenData$: Observable<SpecimenGraph[] | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get<SpecimenGraph[]>(`${this.baseUrl}`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}`, undefined))
          );
      })
    );

  specimenDataWeek$: Observable<HttpResponse<any> | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get(`${this.baseUrl}/daily/WEEK`, {headers: {'Authorization': 'Bearer ' + token}, observe: 'response'})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}/daily/WEEK`, undefined))
          );
      })
    );

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return of(result as T);
    };
  }
}
