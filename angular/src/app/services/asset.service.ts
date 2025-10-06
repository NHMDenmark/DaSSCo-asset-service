import {HttpClient, HttpHeaders} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Asset} from '../types/types';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {catchError, EMPTY, Observable, shareReplay, switchMap} from 'rxjs';
import {FileProxy} from '../utility';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  private thumbnailCache = new Map<string, Observable<Blob>>();
  private readonly proxyUrl = inject(FileProxy) + '/file_proxy';
  private readonly http = inject(HttpClient);
  private readonly oidcService = inject(OidcSecurityService);

  getAssetThumbnail(asset: Asset): Observable<Blob> {
    const cacheKey = `${asset.institution}/${asset.collection}/${asset.asset_guid}`;

    if (this.thumbnailCache.has(cacheKey)) {
      const cachedThumbnail = this.thumbnailCache.get(cacheKey);
      if (cachedThumbnail) {
        return cachedThumbnail;
      }
    }

    const thumbnail$ = this.oidcService.getAccessToken().pipe(
      switchMap((token) =>
        this.http.get<Blob>(
          `${this.proxyUrl}/api/assetfiles/${asset.institution}/${asset.collection}/${asset.asset_guid}/thumbnail`,
          {
            headers: new HttpHeaders().set('Authorization', `bearer ${token}`),
            responseType: 'blob' as 'json'
          }
        )
      ),
      catchError(() => EMPTY),
      shareReplay({bufferSize: 1, refCount: true})
    );

    this.thumbnailCache.set(cacheKey, thumbnail$);
    return thumbnail$;
  }

  clearThumbnailCache(asset: Asset): void {
    const cacheKey = `${asset.institution}/${asset.collection}/${asset.asset_guid}`;
    this.thumbnailCache.delete(cacheKey);
  }

  clearAllThumbnailCache(): void {
    this.thumbnailCache.clear();
  }
}
