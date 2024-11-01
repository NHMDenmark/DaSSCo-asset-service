import { Injectable } from '@angular/core';
//import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient, HttpErrorResponse} from "@angular/common/http";
import {catchError, Observable, switchMap, throwError} from "rxjs";
import {OidcSecurityService} from "angular-auth-oidc-client";

@Injectable({
  providedIn: 'root'
})
export class BulkUpdateService {
  // TODO: GET ASSETS FROM THE FRONTEND! THIS IS JUST MOCK DATA!
  baseUrl = "api/v1/assetmetadata/bulkUpdate?"

  constructor(public oidcSecurityService: OidcSecurityService,
              private http: HttpClient) { }

  updateAssets(updatedFields : Object, assets : String): Observable<any> {
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
          return this.http.put(`${this.baseUrl}${assets}`, updatedFieldsWithUsername, {headers: {'Authorization': 'Bearer ' + token}, observe: 'response'})
            .pipe(
              catchError(this.handleError(`put ${this.baseUrl}`))
            )
        })
      )
  }

  private handleError<T>(operation = 'operation') {
    return (error: HttpErrorResponse): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return throwError(() => error);
    };
  }

}
