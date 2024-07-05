import {Injectable} from '@angular/core';
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {catchError, Observable, of, switchMap} from "rxjs";
import {Institute} from "../types/types";

@Injectable({
  providedIn: 'root'
})
export class InstituteService {
  baseUrl = '/api/v1/institutes';

  constructor(
    public oidcSecurityService: OidcSecurityService
    , private http: HttpClient
  ) { }

  institutes$: Observable<Institute[] | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get<Institute[]>(`${this.baseUrl}`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}`, undefined))
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
