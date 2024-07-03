import {inject, Injectable} from '@angular/core';
import {Asset} from "../types";
import {catchError, Observable, of, switchMap, throwError} from "rxjs";
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {AssetService, FileProxy} from "../utility";

@Injectable({
  providedIn: 'root'
})
export class DetailedViewService {

  constructor(private oidcSecurityService : OidcSecurityService, private http : HttpClient) { }

  private readonly assetUrl = inject(AssetService)
  private readonly proxyUrl = inject(FileProxy)

  getAssetMetadata(assetGuid : string): Observable<Asset | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get<Asset>(`${this.assetUrl}/api/v1/assetmetadata/${assetGuid}`, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.assetUrl}/api/v1/assetmetadata/${assetGuid}`, undefined))
            );
        })
      );
  }

  postAsset(asset: string, institution : string | undefined, collection : string | undefined, assetGuid : string | undefined) : Observable<any> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<string>(`${this.proxyUrl}/file_proxy/api/assetfiles/createCsvFile/${institution}/${collection}/${assetGuid}`, asset, {headers: {'Authorization': 'Bearer ' + token}, responseType: 'text' as 'json', observe: "response"})
            .pipe(
              catchError((error: any) => {
                return throwError(() => error);
              })
          );
        })
      );
  }

  getFile(file : string, institution : string | undefined, collection : string | undefined, assetGuid : string | undefined ) {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get(`${this.proxyUrl}/file_proxy/api/assetfiles/${institution}/${collection}/${assetGuid}/${file}`, { headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.proxyUrl}/file_proxy/api/assetfiles/${institution}/${collection}/${assetGuid}/${file}`, undefined))
            )
        })
      )
  }


  getThumbnail(institution: string, collection: string, assetGuid: string, thumbnail : string) {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get(`${this.proxyUrl}/file_proxy/api/assetfiles/${institution}/${collection}/${assetGuid}/${thumbnail}`, { headers: {'Authorization': 'Bearer ' + token}, responseType: 'blob'})
            .pipe(
              catchError(this.handleError(`get ${this.proxyUrl}/file_proxy/api/assetfiles/${institution}/${collection}/${assetGuid}/${thumbnail}`, undefined))
            )
        })
    )
  }

  getFileList(institution: string, collection: string, assetGuid: string){
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get<string[]>(`${this.proxyUrl}/file_proxy/api/assetfiles/${institution}/${collection}/${assetGuid}`, { headers: {'Authorization': "Bearer " + token}})
            .pipe(
              catchError(this.handleError(`get ${this.proxyUrl}/file_proxy/api/assetfiles/${institution}/${collection}/${assetGuid}`, undefined))
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
