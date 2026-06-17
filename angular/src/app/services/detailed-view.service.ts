import {inject, Injectable} from '@angular/core';
import {catchError, map, Observable, of, switchMap, throwError} from 'rxjs';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {FileProxy} from '../utility';
import {Asset} from '../types/types';
import {AuthService} from './auth.service';

export type AssetMetadataState =
  | {status: 'visible', asset: Asset}
  | {status: 'not-found' | 'forbidden' | 'error', asset?: undefined};

@Injectable({
  providedIn: 'root'
})
export class DetailedViewService {
  private authService = inject(AuthService);
  private http = inject(HttpClient);

  private readonly proxyUrl = inject(FileProxy);
  private getMetadataUrl = 'api/v1/assetmetadata/';
  private createCsvFile = this.proxyUrl + '/file_proxy/api/assetfiles/createCsvFile';
  private assetFiles = this.proxyUrl + '/file_proxy/api/assetfiles/listfiles/';
  private thumbnail = this.proxyUrl + '/file_proxy/api/files/assets/';
  private tempFiles = this.proxyUrl + '/file_proxy/api/assetfiles/getTempFile';
  private deleteTempFolder = this.proxyUrl + '/file_proxy/api/assetfiles/deleteTempFolder';

  getAssetMetadata(assetGuid: string): Observable<AssetMetadataState> {
    return this.authService
      .getAccessToken()
      .pipe(
        switchMap((token) =>
          this.http
            .get<Asset | null>(`${this.getMetadataUrl}${assetGuid}`, {
              headers: {'Authorization': 'Bearer ' + token}
            })
            .pipe(
              map((asset): AssetMetadataState => (asset ? {status: 'visible', asset} : {status: 'not-found'})),
              catchError((error: HttpErrorResponse) => of(this.toAssetMetadataState(error)))
            )
        )
      );
  }

  postCsv(assets: string[]): Observable<any> {
    return this.authService.getAccessToken().pipe(
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

  getFile(guid: string, file: string): Observable<Blob> {
    return this.authService.getAccessToken().pipe(
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
    return this.authService.getAccessToken().pipe(
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
    return this.authService.getAccessToken().pipe(
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
    return this.authService
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
    return this.authService.getAccessToken()
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

  private toAssetMetadataState(error: HttpErrorResponse): AssetMetadataState {
    console.error(error);
    console.error(`get ${this.getMetadataUrl} - ${JSON.stringify(error)}`);

    if (error.status === 403) {
      return {status: 'forbidden'};
    }
    if (error.status === 404) {
      return {status: 'not-found'};
    }
    return {status: 'error'};
  }
}
