import {inject, Injectable} from '@angular/core';
import {catchError, Observable, of, switchMap, throwError} from 'rxjs';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {HttpClient} from '@angular/common/http';
import {FileProxy} from '../utility';
import {Asset} from '../types/types';

@Injectable({
  providedIn: 'root'
})
export class DetailedViewService {
  private oidcSecurityService = inject(OidcSecurityService);
  private http = inject(HttpClient);

  private readonly proxyUrl = inject(FileProxy);
  private getMetadataUrl = 'api/v1/assetmetadata/';
  private createCsvFile = this.proxyUrl + '/file_proxy/api/assetfiles/createCsvFile';
  private createZipFile = this.proxyUrl + '/file_proxy/api/assetfiles/createZipFile';
  private assetFiles = this.proxyUrl + '/file_proxy/api/assetfiles/listfiles/';
  private thumbnail = this.proxyUrl + '/file_proxy/api/files/assets/';
  private tempFiles = this.proxyUrl + '/file_proxy/api/assetfiles/getTempFile';
  private deleteTempFolder = this.proxyUrl + '/file_proxy/api/assetfiles/deleteTempFolder';

  getAssetMetadata(assetGuid: string): Observable<Asset | undefined> {
    return this.oidcSecurityService
      .getAccessToken()
      .pipe(
        switchMap((token) =>
          this.http
            .get<Asset>(`${this.getMetadataUrl}${assetGuid}`, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(catchError(this.handleError(`get ${this.getMetadataUrl}${assetGuid}`, undefined)))
        )
      );
  }

  postCsv(assets: string[]): Observable<any> {
    return this.oidcSecurityService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .post<any>(`${this.createCsvFile}`, assets, {
            headers: {'Authorization': 'Bearer ' + token},
            responseType: 'text' as 'json',
            observe: 'response'
          })
          .pipe(catchError((error: Error) => throwError(() => error)))
      )
    );
  }

  postZip(guid: string, assets: string[]): Observable<any> {
    return this.oidcSecurityService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .post<string>(`${this.createZipFile}/${guid}`, assets, {
            headers: {'Authorization': 'Bearer ' + token},
            responseType: 'text' as 'json',
            observe: 'response'
          })
          .pipe(catchError((error: Error) => throwError(() => error)))
      )
    );
  }

  getFile(guid: string, file: string): Observable<Blob> {
    return this.oidcSecurityService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .get(`${this.tempFiles}/${guid}/${file}`, {
            headers: {'Authorization': 'Bearer ' + token},
            responseType: 'blob'
          })
          .pipe(catchError((error: Error) => throwError(() => error)))
      )
    );
  }

  deleteFile(guid: string) {
    return this.oidcSecurityService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .delete(`${this.deleteTempFolder}/${guid}`, {
            headers: {'Authorization': 'Bearer ' + token},
            observe: 'response'
          })
          .pipe(catchError((error) => throwError(() => error)))
      )
    );
  }

  getThumbnail(institution: string, collection: string, assetGuid: string) {
    return this.oidcSecurityService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .get(`${this.thumbnail}${institution}/${collection}/${assetGuid}/thumbnail`, {
            headers: {'Authorization': 'Bearer ' + token},
            responseType: 'blob'
          })
          .pipe(
            catchError(
              this.handleError(`get ${this.assetFiles}${institution}/${collection}/${assetGuid}/thumbnail`, undefined)
            )
          )
      )
    );
  }

  getFileList(assetGuid: string) {
    return this.oidcSecurityService
      .getAccessToken()
      .pipe(
        switchMap((token) =>
          this.http
            .get<string[]>(`${this.assetFiles}${assetGuid}`, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(catchError(this.handleError(`get ${this.assetFiles}${assetGuid}`, undefined)))
        )
      );
  }

  getFileTicket(asset: Asset) {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => this.http.get(`${this.thumbnail}${asset.asset_guid}/ticket`, {
          headers: {'Authorization': 'Bearer ' + token},
          responseType: 'text'
        }).pipe(
          catchError(this.handleError(`${this.thumbnail}$\{asset.asset_guid}/ticket`, undefined))
        ))
      );
  }
  getLargeDownloadUrl(asset: Asset, ticket: string) {
    return `${this.thumbnail}download/${asset.institution}/${asset.collection}/${asset.asset_guid}?ticket=${ticket}`;
}

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: Error): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return of(result as T);
    };
  }
}
