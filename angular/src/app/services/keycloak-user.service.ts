import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  finalize,
  map,
  Observable,
  of,
  shareReplay,
  switchMap,
  take
} from 'rxjs';
import {KeycloakUserFrontend} from '../types/keycloak-user-frontend';

@Injectable({
  providedIn: 'root'
})
export class KeycloakUserService {
  private readonly http = inject(HttpClient);
  private readonly oidcService = inject(OidcSecurityService);
  private readonly users = new BehaviorSubject<KeycloakUserFrontend[]>([]);
  users$ = this.users.asObservable();
  private readonly loading = new BehaviorSubject(true);
  loading$ = this.loading.asObservable();

  constructor() {
    this.fetchKeycloakUsersForGroup('digitiser')
      .pipe(
        take(1),
        catchError((e: Error) => {
          console.error('Error loading keycloak users', e);
          return of([] as KeycloakUserFrontend[]);
        }),
        finalize(() => this.loading.next(false))
      )
      .subscribe((users) => this.users.next(users));
  }

  getFilteredKeycloakUsers(search$: Observable<string>) {
    return combineLatest([this.users$, search$]).pipe(map(([users, search]) => this.filterUsers(users, search)));
  }

  private fetchKeycloakUsersForGroup(group: string) {
    return this.oidcService.getAccessToken().pipe(
      switchMap((token) =>
        this.http.get<KeycloakUserFrontend[]>('api/v1/assetgroups/keycloak/users', {
          params: {group},
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
      ),
      shareReplay({bufferSize: 1, refCount: true})
    );
  }

  private filterUsers(users: KeycloakUserFrontend[], search: string | null | undefined) {
    const normalizedSearch = search?.trim().toLowerCase();
    if (!normalizedSearch) return users;

    return users.filter((user) =>
      [user.username, user.firstName, user.lastName]
        .filter((value): value is string => !!value)
        .some((value) => value.toLowerCase().includes(normalizedSearch))
    );
  }
}
