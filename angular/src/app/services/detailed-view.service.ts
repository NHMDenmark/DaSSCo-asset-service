import {inject, Injectable} from '@angular/core';
import {Asset} from "../types/query-types";
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

  private getMetadataUrl = this.assetUrl + "/api/v1/assetmetadata/";
  private createCsvFile = this.proxyUrl + "/file_proxy/api/assetfiles/createCsvFile/";
  private createZipFile = this.proxyUrl + "/file_proxy/api/assetfiles/createZipFile/";
  private assetFiles = this.proxyUrl + "/file_proxy/api/assetfiles/";
  private deleteLocalFiles = this.proxyUrl + "/file_proxy/api/assetfiles/deleteLocalFiles/";

  getAssetMetadata(assetGuid : string): Observable<Asset | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get<Asset>(`${this.getMetadataUrl}${assetGuid}`, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.getMetadataUrl}${assetGuid}`, undefined))
            );
        })
      );
  }

  postCsv(asset: string, institution : string | undefined, collection : string | undefined, assetGuid : string | undefined) : Observable<any> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<string>(`${this.createCsvFile}${institution}/${collection}/${assetGuid}`, asset, {headers: {'Authorization': 'Bearer ' + token}, responseType: 'text' as 'json', observe: "response"})
            .pipe(
              catchError((error: any) => {
                return throwError(() => error);
              })
          );
        })
      );
  }

  postZip(asset: string, institution : string | undefined, collection : string | undefined, assetGuid : string | undefined) : Observable<any> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<string>(`${this.createZipFile}${institution}/${collection}/${assetGuid}`, asset, {headers: {'Authorization': 'Bearer ' + token}, responseType: 'text' as 'json', observe: "response" })
            .pipe(
              catchError((error: any) => {
                return throwError(() => error);
              })
            );
        })
      );
  }

  getFile(file : string, institution : string | undefined, collection : string | undefined, assetGuid : string | undefined ) : Observable<Blob> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get(`${this.assetFiles}${institution}/${collection}/${assetGuid}/${file}`, { headers: {'Authorization': 'Bearer ' + token}, responseType: "blob"})
            .pipe(
              catchError((error: any) => {
                return throwError(() => error)}))
        })
      )
  }

  deleteFile(file : string, institution : string | undefined, collection : string | undefined, assetGuid : string | undefined) {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.delete(`${this.deleteLocalFiles}${institution}/${collection}/${assetGuid}/${file}`, {headers: {'Authorization': 'Bearer ' + token}, observe: "response"})
            .pipe(
              catchError((error) => {
                return throwError(() => error)
              })
            )
        })
      )
  }


  getThumbnail(institution: string, collection: string, assetGuid: string, thumbnail : string) {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get(`${this.assetFiles}${institution}/${collection}/${assetGuid}/${thumbnail}`, { headers: {'Authorization': 'Bearer ' + token}, responseType: 'blob'})
            .pipe(
              catchError(this.handleError(`get ${this.assetFiles}${institution}/${collection}/${assetGuid}/${thumbnail}`, undefined))
            )
        })
    )
  }

  getFileList(institution: string, collection: string, assetGuid: string){
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.get<string[]>(`${this.assetFiles}${institution}/${collection}/${assetGuid}`, { headers: {'Authorization': "Bearer " + token}})
            .pipe(
              catchError(this.handleError(`get ${this.assetFiles}${institution}/${collection}/${assetGuid}`, undefined))
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
