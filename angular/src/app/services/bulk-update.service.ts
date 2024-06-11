import { Injectable } from '@angular/core';
//import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {catchError, Observable, of, switchMap} from "rxjs";
import {OidcSecurityService} from "angular-auth-oidc-client";

@Injectable({
  providedIn: 'root'
})
export class BulkUpdateService {

  baseUrl = "/api/v1/assetmetadata/bulkUpdate?assets=testasset"

  constructor(public oidcSecurityService: OidcSecurityService,
              private http: HttpClient) { }


  updateAssets(updatedFields : string): Observable<any> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.put(`${this.baseUrl}`, updatedFields, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`put ${this.baseUrl}`, undefined))
            )
        })
      )
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return of(result as T);
    };
  }

}
