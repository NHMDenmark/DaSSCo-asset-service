import { Injectable } from '@angular/core';
//import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {catchError, Observable, of, switchMap} from "rxjs";
import {OidcSecurityService} from "angular-auth-oidc-client";

@Injectable({
  providedIn: 'root'
})
export class BulkUpdateService {
  // TODO: GET ASSETS FROM THE FRONTEND! THIS IS JUST MOCK DATA!
  baseUrl = "/api/v1/assetmetadata/bulkUpdate?assets=test-1&assets=test-2"

  constructor(public oidcSecurityService: OidcSecurityService,
              private http: HttpClient) { }

  updateAssets(updatedFields : Object): Observable<any> {
    let username;
    this.oidcSecurityService.checkAuth().subscribe(({isAuthenticated, userData}) => {
      if (isAuthenticated && userData){
        username = userData.preferred_username;
      }
    })

    const updatedFieldsWithUsername = {
      ...updatedFields, updateUser: username
    }

    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.put(`${this.baseUrl}`, updatedFieldsWithUsername, {headers: {'Authorization': 'Bearer ' + token}})
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
