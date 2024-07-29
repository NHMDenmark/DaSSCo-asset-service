import { Injectable } from '@angular/core';
import {catchError, Observable, of, switchMap} from "rxjs";
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {AssetGroup} from "../types/types";

@Injectable({
  providedIn: 'root'
})
export class AssetGroupService {
  baseUrl = '/api/v1/assetgroups';

  constructor(public oidcSecurityService: OidcSecurityService
    , private http: HttpClient) { }

  assetGroups$: Observable<AssetGroup[] | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get<AssetGroup[]>(`${this.baseUrl}/`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}/`, undefined))
          );
      })
    );

  newGroup(group: AssetGroup): Observable<AssetGroup | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.post<AssetGroup>(`${this.baseUrl}/createassetgroup/`, group, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/createassetgroup`, undefined))
            );
        })
      );
  }

  updateGroupAddAssets(groupName: string | undefined, assets: string[]): Observable<AssetGroup | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.put<AssetGroup>(`${this.baseUrl}/updategroup/${groupName}/addAssets`, assets, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/updategroup/${groupName}/addAssets`, undefined))
            );
        })
      );
  }

  updateGroupRemoveAssets(groupName: string | undefined, assets: string[]): Observable<AssetGroup | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.put<AssetGroup>(`${this.baseUrl}/updategroup/${groupName}/removeAssets`, assets, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/updategroup/${groupName}/removeAssets`, undefined))
            );
        })
      );
  }

  grantAccess(groupName: string | undefined, users: string[]): Observable<AssetGroup | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.put<AssetGroup>(`${this.baseUrl}/grantAccess/${groupName}`, users, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/grantAccess/${groupName}`, undefined))
            );
        })
      );
  }

  revokeAccess(groupName: string | undefined, users: string[]): Observable<AssetGroup | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.put<AssetGroup>(`${this.baseUrl}/revokeAccess/${groupName}`, users, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/revokeAccess/${groupName}`, undefined))
            );
        })
      );
  }

  deleteGroup(groupName: string | undefined): Observable<any | undefined> {
    return this.oidcSecurityService.getAccessToken()
      .pipe(
        switchMap((token) => {
          return this.http.delete(`${this.baseUrl}/deletegroup/${groupName}`, {headers: {'Authorization': 'Bearer ' + token}})
            .pipe(
              catchError(this.handleError(`get ${this.baseUrl}/deletegroup/${groupName}`, undefined))
            );
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
