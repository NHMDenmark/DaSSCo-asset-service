import {inject, Injectable} from '@angular/core';
import {Asset} from "../types";
import {catchError, Observable, of, switchMap} from "rxjs";
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {AssetService} from "../utility";

@Injectable({
  providedIn: 'root'
})
export class DetailedViewService {

  constructor(private oidcSecurityService : OidcSecurityService, private http : HttpClient) { }

  private readonly assetUrl = inject(AssetService)

  getAssetMetadata(): Observable<Asset | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get<Asset>(`${this.assetUrl}/api/v1/assetmetadata/test-1`, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.assetUrl}/api/v1/assetmetadata/test-1`, undefined))
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
