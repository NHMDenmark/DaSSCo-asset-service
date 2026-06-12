import {HttpClient, HttpHeaders} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Asset, QueryResultAsset} from '../types/types';
import {catchError, EMPTY, map, Observable, shareReplay, switchMap} from 'rxjs';
import {FileProxy} from '../utility';
import {AuthService} from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  private thumbnailCache = new Map<string, Observable<string>>();
  private readonly proxyUrl = inject(FileProxy) + '/file_proxy';
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  getAssetThumbnail(asset: QueryResultAsset): Observable<string> {
    const cacheKey = `${asset.institution}/${asset.collection}/${asset.asset_guid}/${asset.events?.length ?? 0}`;
    if (this.thumbnailCache.has(cacheKey)) {
      const cachedThumbnail = this.thumbnailCache.get(cacheKey);
      if (cachedThumbnail) {
        return cachedThumbnail;
      }
    }

    const thumbnail$ = this.authService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .get<Blob>(
            `${this.proxyUrl}/api/files/assets/${asset.institution}/${asset.collection}/${asset.asset_guid}/thumbnail`,
            {
              headers: new HttpHeaders().set('Authorization', `bearer ${token}`),
              responseType: 'blob' as 'json'
            }
          )
          .pipe(
            map((blob) => {
              const url = URL.createObjectURL(blob);
              return url;
            })
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
