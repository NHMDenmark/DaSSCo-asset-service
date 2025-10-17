import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {catchError, map, Observable, of} from 'rxjs';
import {PublicAssetMetadata} from 'src/app/types/types';
import {AssetService, FileProxy} from 'src/app/utility';

@Injectable({
  providedIn: 'root'
})
export class ExternDetailedViewService {
  private http = inject(HttpClient);
  private url = inject(AssetService);
  private proxyUrl = inject(FileProxy);
  private thumbnail = this.proxyUrl + '/file_proxy/api/files/assets/';

  getAssetMetaData(assetGuid: string) {
    return this.http
      .get<PublicAssetMetadata>(`${this.url}api/extern/metadata/${assetGuid}`)
      .pipe(catchError(() => of(null)));
  }

  getThumbnail(institution?: string, collection?: string, assetGuid?: string) {
    if (!institution || !collection || !assetGuid) {
      return of(undefined);
    }
    return this.http
      .get(`${this.thumbnail}${institution}/${collection}/${assetGuid}/thumbnail`, {
        responseType: 'blob'
      })
      .pipe(
        catchError(
          this.handleError(`get ${this.thumbnail}${institution}/${collection}/${assetGuid}/thumbnail`, undefined)
        )
      );
  }

  checkAccess(asset_guid: string) {
    return this.http.post<void>(`${this.url}api/v1/assets/readaccess`, undefined, {
      params: {
        assetGuid: asset_guid
      }
    }).pipe(
      map(() => true),
      catchError((err) => {
        console.log(err)
        return of(false)
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
