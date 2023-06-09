import {Injectable} from '@angular/core';
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient, HttpResponse} from "@angular/common/http";
import {catchError, Observable, of, switchMap} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class SpecimenGraphService {
  baseUrl = '/api/v1/graphdata';

  constructor(
    public oidcSecurityService: OidcSecurityService
    , private http: HttpClient
  ) { }

  specimenDataWeek$: Observable<HttpResponse<any> | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get(`${this.baseUrl}/WEEK`, {headers: {'Authorization': 'Bearer ' + token}, observe: 'response'})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}/daily/WEEK`, undefined))
          );
      })
    );

  specimenDataMonth$: Observable<HttpResponse<any> | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get(`${this.baseUrl}/MONTH`, {headers: {'Authorization': 'Bearer ' + token}, observe: 'response'})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}/daily/MONTH`, undefined))
          );
      })
    );

  specimenDataYear$: Observable<HttpResponse<any> | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get(`${this.baseUrl}/YEAR`, {headers: {'Authorization': 'Bearer ' + token}, observe: 'response'})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}/year`, undefined))
          );
      })
    );

  getSpecimenDataCustom(view: string, start: number, end: number): Observable<HttpResponse<any> | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get(`${this.baseUrl}/custom?view=${view}&start=${start}&end=${end}`, {headers: {'Authorization': 'Bearer ' + token}, observe: 'response'})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/custom`, undefined))
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
