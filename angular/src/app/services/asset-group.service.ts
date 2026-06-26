import {Injectable} from '@angular/core';
import {catchError, Observable, of, switchMap} from 'rxjs';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {AssetGroup, DasscoError} from '../types/types';
import {KeycloakUserFrontend} from '../types/keycloak-user-frontend';
import {AuthService} from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AssetGroupService {
  baseUrl = 'api/v1/assetgroups';

  constructor(private authService: AuthService, private http: HttpClient) {}

  ownAssetGroups$: Observable<AssetGroup[] | undefined> = this.authService.getAccessToken().pipe(
    switchMap((token) => this.http
        .get<AssetGroup[]>(`${this.baseUrl}/owned`, {headers: {'Authorization': 'Bearer ' + token}})
        .pipe(catchError(this.handleError(`get ${this.baseUrl}/owned`, undefined))))
  );

  assetGroups$: Observable<AssetGroup[] | undefined> = this.authService.getAccessToken().pipe(
    switchMap((token) => this.http
        .get<AssetGroup[]>(`${this.baseUrl}/`, {headers: {'Authorization': 'Bearer ' + token}})
        .pipe(catchError(this.handleError(`get ${this.baseUrl}/`, undefined))))
  );

  newGroup(group: AssetGroup): Observable<AssetGroup | DasscoError | undefined> {
    return this.authService.getAccessToken().pipe(
      switchMap((token) => this.http
          .post<AssetGroup>(`${this.baseUrl}/createassetgroup/`, group, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(catchError((error) => of((error as HttpErrorResponse).error as DasscoError))))
    );
  }

  updateGroupAddAssets(
    groupId: number | undefined,
    assets: string[]
  ): Observable<AssetGroup | DasscoError | undefined> {
    return this.authService.getAccessToken().pipe(
      switchMap((token) => this.http
          .put<AssetGroup>(`${this.baseUrl}/updategroup/${groupId}/addAssets`, assets, {
            headers: {'Authorization': 'Bearer ' + token}
          })
          .pipe(catchError((error) => of((error as HttpErrorResponse).error as DasscoError))))
    );
  }

  getKeyCloakUsers() {
    return this.authService.getAccessToken()
      .pipe(
        switchMap((token) => this.http.get<KeycloakUserFrontend[]>(this.baseUrl + '/keycloak/users', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        }).pipe(catchError(this.handleError<KeycloakUserFrontend[]>(`get ${this.baseUrl}/keycloak/users`, []))))
      );
  }

  updateGroupRemoveAssets(groupId: number | undefined, assets: string[]): Observable<AssetGroup | undefined> {
    return this.authService.getAccessToken().pipe(
      switchMap((token) => this.http
          .put<AssetGroup>(`${this.baseUrl}/updategroup/${groupId}/removeAssets`, assets, {
            headers: {'Authorization': 'Bearer ' + token}
          })
          .pipe(catchError(this.handleError(`get ${this.baseUrl}/updategroup/${groupId}/removeAssets`, undefined))))
    );
  }

  grantAccess(groupId: number | undefined, users: KeycloakUserFrontend[]): Observable<AssetGroup | DasscoError | undefined> {
    return this.authService.getAccessToken().pipe(
      switchMap((token) => this.http
          .put<AssetGroup>(`${this.baseUrl}/keycloak/grantAccess/${groupId}`, users, {
            headers: {'Authorization': 'Bearer ' + token}
          })
          .pipe(catchError((error) => of((error as HttpErrorResponse).error as DasscoError))))
    );
  }

  revokeAccess(groupId: number | undefined, users: string[]): Observable<AssetGroup | undefined> {
    return this.authService.getAccessToken().pipe(
      switchMap((token) => this.http
          .put<AssetGroup>(`${this.baseUrl}/revokeAccess/${groupId}`, users, {
            headers: {'Authorization': 'Bearer ' + token}
          })
          .pipe(catchError(this.handleError(`get ${this.baseUrl}/revokeAccess/${groupId}`, undefined))))
    );
  }

  deleteGroup(groupId: number | undefined): Observable<boolean | DasscoError | undefined> {
    return this.authService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .delete<boolean>(`${this.baseUrl}/${groupId}`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(catchError((error) => of((error as HttpErrorResponse).error as DasscoError)))
      )
    );
  }

  deleteGroups(groupIds: number[]): Observable<boolean | DasscoError | undefined> {
    return this.authService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .post<boolean>(`${this.baseUrl}/bulk-delete`, groupIds, {
            headers: {'Authorization': 'Bearer ' + token}
          })
          .pipe(catchError((error) => of((error as HttpErrorResponse).error as DasscoError)))
      )
    );
  }

  bulkAuditAssets(assetGuids: string[], user: string): Observable<{[key: string]: string} | DasscoError | undefined> {
    return this.authService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .post<{[key: string]: string}>(
            'api/v1/assetmetadata/bulk/audit',
            {user, assetGuids},
            {
              headers: {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json'}
            }
          )
          .pipe(catchError((error) => of((error as HttpErrorResponse).error as DasscoError)))
      )
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
