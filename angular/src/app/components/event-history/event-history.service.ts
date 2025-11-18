import {HttpClient, HttpParams} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {BehaviorSubject, catchError, combineLatest, distinctUntilChanged, of, switchMap, tap} from 'rxjs';
import {AssetService} from 'src/app/utility';
import {PaginatedEventsResponse} from './event-history.model';
import {OidcSecurityService} from 'angular-auth-oidc-client';

@Injectable({
  providedIn: 'root'
})
export class EventHistoryService {
  private readonly http = inject(HttpClient);
  private readonly assetServiceUrl = inject(AssetService);
  private readonly oidcService = inject(OidcSecurityService);

  private readonly page = new BehaviorSubject<number>(1);
  private readonly limit = new BehaviorSubject<number>(100);
  private readonly direction = new BehaviorSubject<'DESC' | 'ASC'>('DESC');
  private readonly eventType = new BehaviorSubject<Event['type']>('BULK_UPDATE_ASSET_METADATA');
  private readonly startDate = new BehaviorSubject<Date | undefined>(undefined);
  private readonly endDate = new BehaviorSubject<Date | undefined>(undefined);
  private readonly loading = new BehaviorSubject<boolean>(false);

  readonly loading$ = this.loading.asObservable();
  readonly page$ = this.page.asObservable();
  readonly limit$ = this.limit.asObservable();
  readonly direction$ = this.direction.asObservable();
  readonly startDate$ = this.startDate.asObservable();
  readonly endDate$ = this.endDate.asObservable();
  readonly eventType$ = this.eventType.asObservable();

  readonly eventTypes$ = this.getEventTypes();
  readonly events$ = combineLatest({
    page: this.page,
    limit: this.limit,
    direction: this.direction,
    eventType: this.eventType,
    startDate: this.startDate,
    endDate: this.endDate
  }).pipe(
    distinctUntilChanged(
      (prev, curr) =>
        prev.page === curr.page &&
        prev.limit === curr.limit &&
        prev.direction === curr.direction &&
        prev.eventType === curr.eventType &&
        prev.startDate === curr.startDate &&
        prev.endDate === curr.endDate
    ),
    tap(() => this.loading.next(true)),
    switchMap(({page, limit, direction, eventType, startDate, endDate}) =>
      this.getEventsPaginated(page, limit, direction, eventType, startDate, endDate).pipe(
        tap({next: () => this.loading.next(false), error: () => this.loading.next(false)})
      )
    )
  );

  getEventsPaginated(
    page: number,
    limit: number,
    direction: 'DESC' | 'ASC',
    eventType: Event['type'],
    startDate: Date | undefined = undefined,
    endDate: Date | undefined = undefined
  ) {
    let params = new HttpParams().appendAll({
      page,
      limit,
      direction,
      eventType
    });
    if (startDate) {
      params = params.append('startDate', startDate.toISOString());
    }
    if (endDate) {
      params = params.append('endDate', endDate.toISOString());
    }
    return this.oidcService.getAccessToken().pipe(
      switchMap((token) =>
        this.http.get<PaginatedEventsResponse>(`${this.assetServiceUrl}/api/v1/events`, {
          headers: {
            'Authorization': `Bearer ${token}`
          },
          params
        })
      )
    );
  }

  setPage(page: number) {
    this.page.next(page);
  }

  setLimit(limit: number) {
    this.limit.next(limit);
  }

  setDirection(direction: 'DESC' | 'ASC') {
    this.direction.next(direction);
  }

  setEventType(eventType: Event['type']) {
    this.eventType.next(eventType);
  }

  setStartDate(startDate: Date | undefined) {
    this.startDate.next(startDate);
  }

  setEndDate(endDate: Date | undefined) {
    this.endDate.next(endDate);
  }

  getEventTypes() {
    return this.oidcService.getAccessToken().pipe(
      switchMap((token) =>
        this.http
          .get<string[]>(`${this.assetServiceUrl}/api/v1/events/types`, {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          })
          .pipe(
            catchError((error) => {
              console.error(error);
              return of([]);
            })
          )
      )
    );
  }
}
