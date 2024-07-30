import {inject, Injectable} from '@angular/core';
import {catchError, Observable, of, switchMap, throwError} from "rxjs";
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {AssetService, FileProxy} from "../utility";
import {Asset} from "../types/types";

@Injectable({
  providedIn: 'root'
})
export class DetailedViewService {

  constructor(private oidcSecurityService : OidcSecurityService, private http : HttpClient) { }

  private readonly assetUrl = inject(AssetService)
  private readonly proxyUrl = inject(FileProxy)

  private getMetadataUrl = this.assetUrl + "/api/v1/assetmetadata/";
  private createCsvFile = this.proxyUrl + "/file_proxy/api/assetfiles/createCsvFile";
  private createZipFile = this.proxyUrl + "/file_proxy/api/assetfiles/createZipFile/";
  private assetFiles = this.proxyUrl + "/file_proxy/api/assetfiles/";
  private tempFiles = this.proxyUrl + "/file_proxy/api/assetfiles/getTempFile"
  private deleteTempFolder = this.proxyUrl + "/file_proxy/api/assetfiles/deleteTempFolder";

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

  postCsv(assets : string[]) : Observable<any> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<any>(`${this.createCsvFile}`, assets, {headers: {'Authorization': 'Bearer ' + token}, responseType: 'text' as 'json', observe: "response"})
            .pipe(
              catchError((error: any) => {
                return throwError(() => error);
              })
          );
        })
      );
  }

  postZip(asset: string, institution : string | undefined, collection : string | undefined, assetGuid : string | undefined) : Observable<any> {
    // check permission for creating the zip file
    // if not, return 403 and information about the failed assets
    // if yes, get images from the assets that have assets, save them in temp in different folders based on the a_guid
    // create the zip file
    // download
    // profit
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

  getFile(file : string) : Observable<Blob> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          console.log(`${this.tempFiles}/${file}`)
          return this.http.get(`${this.tempFiles}/${file}`, { headers: {'Authorization': 'Bearer ' + token}, responseType: "blob"})
            .pipe(
              catchError((error: any) => {
                return throwError(() => error)}))
        })
      )
  }

  deleteFile() {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.delete(`${this.deleteTempFolder}`, {headers: {'Authorization': 'Bearer ' + token}, observe: "response"})
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
