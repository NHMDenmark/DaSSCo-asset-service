import {HttpClient, HttpHeaders} from '@angular/common/http';
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

  getAssetMetaData(assetGuid: string) {
    return this.http
      .get<PublicAssetMetadata>(`${this.url}/api/extern/metadata/${assetGuid}`)
      .pipe(catchError(() => of(null)));
  }

  getAssetFileList(assetGuid?: string) {
    if (!assetGuid) {
      return of([]);
    }
    return this.http
      .get<string[]>(`${this.proxyUrl}/file_proxy/api/assetfiles/listfiles/${assetGuid}`)
      .pipe(
        catchError(
          this.handleError(
            `get ${this.proxyUrl}/file_proxy/api/files/assetfiles/listfiles/${assetGuid}`,
            [] as string[]
          )
        )
      );
  }

  getThumbnail(institution?: string, collection?: string, assetGuid?: string) {
    if (!institution || !collection || !assetGuid) {
      return of(undefined);
    }
    return this.http
      .get(`${this.proxyUrl}/file_proxy/api/files/assets/${institution}/${collection}/${assetGuid}/thumbnail`, {
        responseType: 'blob'
      })
      .pipe(
        catchError(
          this.handleError(
            `get ${this.proxyUrl}/file_proxy/api/files/assets/${institution}/${collection}/${assetGuid}/thumbnail`,
            undefined
          )
        )
      );
  }

  downloadMetadataCsv(assetGuid: string): Observable<Blob> {
    const url = `${this.url}api/extern/metadata/${assetGuid}/csv`;
    const headers = new HttpHeaders({Accept: 'text/csv'});

    return this.http.get(url, {
      headers,
      responseType: 'blob'
    });
  }
  downloadAssetBundle(institution: string, collection: string, assetGuid: string): Observable<Blob> {
    const url = `${this.proxyUrl}/file_proxy/api/files/assets/extern/${institution}/${collection}/${assetGuid}/zip`;
    const headers = new HttpHeaders({Accept: 'application/zip'});

    return this.http.get(url, {
      headers,
      responseType: 'blob'
    });
  }

  triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  checkAccess(asset_guid: string) {
    return this.http
      .post<void>(`${this.url}/api/v1/assets/readaccess`, undefined, {
        params: {
          assetGuid: asset_guid
        }
      })
      .pipe(
        map(() => true),
        catchError((err) => {
          console.log(err);
          return of(false);
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
