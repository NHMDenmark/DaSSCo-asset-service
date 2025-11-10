import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {catchError, Observable, throwError} from 'rxjs';
import {AssetService} from '../utility';
import {Digitiser, Funding} from '../types/types';

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
  issues?: IssuePatchBlock;
  digitisers?: DigitiserPatchBlock;
}

export interface AssetPatchFields {
  asset_locked: boolean;
  audited: boolean;
  funding: number;
  asset_subject: string;
  status: string;
  camera_setting_control: string;
  metadata_source: string;
  push_to_specify: boolean;
  role_restrictions: string[];
  payload_type: string;
  legality: {
    copyright: string;
    license: string;
    credit: string;
  };
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

@Injectable({
  providedIn: 'root'
})
export class BulkUpdateService {
  private readonly http = inject(HttpClient);
  private apiUrl = inject(AssetService);

  getDigitiserList() {
    return this.http
      .get<Digitiser[]>(`${this.apiUrl}api/v1/assets/bulkupdate/digitisers`)
      .pipe(catchError(this.handleError<Digitiser[]>('getDigitiserList')));
  }

  getFundingList() {
    return this.http
      .get<Funding[]>(`${this.apiUrl}api/v1/assets/bulkupdate/funding`)
      .pipe(catchError(this.handleError<Funding[]>('getFundingList')));
  }
  getSubjects() {
    return this.http
      .get<string[]>(`${this.apiUrl}api/v1/assets/bulkupdate/subjects`)
      .pipe(catchError(this.handleError<string[]>('getSubjects')));
  }
  getRoles() {
    return this.http
      .get<string[]>(`${this.apiUrl}api/v1/assets/bulkupdate/roles`)
      .pipe(catchError(this.handleError<string[]>('getRoles')));
  }
  getIssueCategories() {
    return this.http
      .get<string[]>(`${this.apiUrl}api/v1/assets/bulkupdate/issue-categories`)
      .pipe(catchError(this.handleError<string[]>('getIssueCategories')));
  }
  getStatuses() {
    return this.http
      .get<string[]>(`${this.apiUrl}api/v1/assets/bulkupdate/statuses`)
      .pipe(catchError(this.handleError<string[]>('getStatuses')));
  }

  getGroupedIssues(assetGuids: string[]) {
    return this.http
      .post<GroupedIssue[]>(`${this.apiUrl}api/v1/assets/bulkupdate/issues/grouped`, assetGuids)
      .pipe(catchError(this.handleError<GroupedIssue[]>('getGroupedIssues')));
  }
  getGroupedDigitisers(assetGuids: string[]): Observable<GroupedDigitiser[]> {
    return this.http
      .post<GroupedDigitiser[]>(`${this.apiUrl}api/v1/assets/bulkupdate/digitisers/grouped`, assetGuids)
      .pipe(catchError(this.handleError<GroupedDigitiser[]>('getGroupedDigitisers')));
  }

  private handleError<T>(operation = 'operation') {
    return (error: HttpErrorResponse): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return throwError(() => error);
    };
  }
}
