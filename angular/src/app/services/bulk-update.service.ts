import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {catchError, Observable, switchMap, throwError} from 'rxjs';
import {AssetService} from '../utility';
import {Digitiser, Funding, Legality} from '../types/types';
import {OidcSecurityService} from 'angular-auth-oidc-client';

export interface GroupedDigitiser {
  dasscoUserId: number;
  username: string;
  digitiserListIds: number[];
  assetGuids: string[];
  count: number;
}

export interface GroupedIssue {
  category: string;
  name: string;
  description: string;
  status: string;
  solved: boolean;
  notes: string;
  issueIds: number[];
  assetGuids: string[];
  count: number;
}

export interface BulkIssueAction {
  issueIds: number[];
  action: 'update' | 'delete';
  values?: Partial<Pick<GroupedIssue, 'category' | 'name' | 'description' | 'status' | 'solved' | 'notes'>>;
}

export interface BulkIssueActionResult {
  issueIds: number[];
  updatedCount: number;
  assetGuids: string[];
  message?: string;
}

export interface BulkUpdatePayload {
  assetGuids: string[];
  fields?: Partial<AssetPatchFields>;
  funding?: number[];
  issues?: IssuePatchBlock;
  legality?: Partial<Legality>;
  roleRestrictions?: RoleRestrictionPatchBlock;
  digitisers?: DigitiserPatchBlock;
}

export interface AssetPatchFields {
  asset_locked: boolean;
  audited: boolean;
  subject: string;
  status: string;
  digitiser_id: number;
  camera_setting_control: string;
  metadata_source: string;
  push_to_specify: boolean;
  payload_type: string;
}

export interface IssuePatchBlock {
  add?: Array<Omit<GroupedIssue, 'issueIds' | 'assetGuids' | 'count'>>;
  update?: Array<BulkIssueAction>;
  delete?: number[];
}

export interface DigitiserPatchBlock {
  add?: Array<{dasscoUserId: number; assetGuids: string[]}>;
  delete?: number[];
}

export interface RoleRestrictionPatchBlock {
  add?: string[];
  delete?: string[];
}

export interface GroupedRoleRestriction {
  role: string;
  assetGuids: string[];
  count: number;
}

@Injectable({
  providedIn: 'root'
})
export class BulkUpdateService {
  private readonly http = inject(HttpClient);
  private apiUrl = inject(AssetService);
  private oidcService = inject(OidcSecurityService);

  getDigitiserList() {
    return this.http
      .get<Digitiser[]>(`${this.apiUrl}/api/v1/assets/bulkupdate/digitisers`)
      .pipe(catchError(this.handleError<Digitiser[]>('getDigitiserList')));
  }

  getFundingList() {
    return this.http
      .get<Funding[]>(`${this.apiUrl}/api/v1/assets/bulkupdate/funding`)
      .pipe(catchError(this.handleError<Funding[]>('getFundingList')));
  }
  getSubjects() {
    return this.http
      .get<string[]>(`${this.apiUrl}/api/v1/assets/bulkupdate/subjects`)
      .pipe(catchError(this.handleError<string[]>('getSubjects')));
  }
  getRoles() {
    return this.http
      .get<string[]>(`${this.apiUrl}/api/v1/assets/bulkupdate/roles`)
      .pipe(catchError(this.handleError<string[]>('getRoles')));
  }
  getIssueCategories() {
    return this.http
      .get<string[]>(`${this.apiUrl}/api/v1/assets/bulkupdate/issue-categories`)
      .pipe(catchError(this.handleError<string[]>('getIssueCategories')));
  }
  getStatuses() {
    return this.http
      .get<string[]>(`${this.apiUrl}/api/v1/assets/bulkupdate/statuses`)
      .pipe(catchError(this.handleError<string[]>('getStatuses')));
  }

  getGroupedIssues(assetGuids: string[]) {
    return this.oidcService.getAccessToken().pipe(
      switchMap((token: string) =>
        this.http
          .post<GroupedIssue[]>(`${this.apiUrl}/api/v1/assets/bulkupdate/issues/grouped`, assetGuids, {
            headers: {'Authorization': 'Bearer ' + token}
          })
          .pipe(catchError(this.handleError<GroupedIssue[]>('getGroupedIssues')))
      )
    );
  }

  getGroupedDigitisers(assetGuids: string[]) {
    return this.oidcService.getAccessToken().pipe(
      switchMap((token: string) =>
        this.http
          .post<GroupedDigitiser[]>(`${this.apiUrl}/api/v1/assets/bulkupdate/digitisers/grouped`, assetGuids, {
            headers: {'Authorization': 'Bearer ' + token}
          })
          .pipe(catchError(this.handleError<GroupedDigitiser[]>('getGroupedDigitisers')))
      )
    );
  }

  getGroupedRoleRestrictions(assetGuids: string[]) {
    return this.oidcService.getAccessToken().pipe(
      switchMap((token: string) =>
        this.http
          .post<GroupedRoleRestriction[]>(
            `${this.apiUrl}/api/v1/assets/bulkupdate/role-restrictions/grouped`,
            assetGuids,
            {headers: {'Authorization': 'Bearer ' + token}}
          )
          .pipe(catchError(this.handleError<GroupedRoleRestriction[]>('getGroupedRoleRestrictions')))
      )
    );
  }
  bulkUpdate(payload: BulkUpdatePayload) {
    return this.oidcService.getAccessToken().pipe(
      switchMap((token: string) =>
        this.http
          .patch<Record<'bulkUpdateUuid', string>>(`${this.apiUrl}/api/v1/assets/bulkupdate`, payload, {
            headers: {'Authorization': 'Bearer ' + token}
          })
          .pipe(catchError(this.handleError<Record<'bulkUpdateUuid', string>>('bulkUpdate')))
      )
    );
  }

  private handleError<T>(operation = 'operation') {
    return (error: HttpErrorResponse): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return throwError(() => error);
    };
  }
}
