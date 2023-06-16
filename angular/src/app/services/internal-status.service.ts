import {Injectable} from '@angular/core';
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient, HttpResponse} from "@angular/common/http";
import {catchError, Observable, of, switchMap} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class InternalStatusService {
  baseUrl = '/api/v1/assets';
  fiveMinutes = 5 * 60 * 1000;

  constructor(
    public oidcSecurityService: OidcSecurityService
    , private http: HttpClient
  ) { }

  dailyInternalStatuses$: Observable<HttpResponse<any> | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get(`${this.baseUrl}/internalstatus/daily`, {headers: {'Authorization': 'Bearer ' + token}, observe: 'response'})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}/internalstatus/daily`, undefined))
          );
      })
    );

  totalInternalStatuses$: Observable<HttpResponse<any> | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get(`${this.baseUrl}/internalstatus/total`, {headers: {'Authorization': 'Bearer ' + token}, observe: 'response'})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}/internalstatus`, undefined))
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
