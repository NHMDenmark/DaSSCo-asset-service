import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {OidcSecurityService} from 'angular-auth-oidc-client';
import {combineLatest, map, Observable, shareReplay, startWith, switchMap} from 'rxjs';
import {KeycloakUserFrontend} from '../types/keycloak-user-frontend';

@Injectable({
  providedIn: 'root'
})
export class KeycloakUserService {
  private readonly http = inject(HttpClient);
  private readonly oidcService = inject(OidcSecurityService);
  private readonly usersByGroup = new Map<string, Observable<KeycloakUserFrontend[]>>();
  users$ = this.getKeycloakUsersForGroup('digitiser');

  getKeycloakUsersForGroup(group = 'digitiser') {
    if (!this.usersByGroup.has(group)) {
      this.usersByGroup.set(group, this.fetchKeycloakUsersForGroup(group));
    }

    return this.usersByGroup.get(group)!;
  }

  getFilteredKeycloakUsers(search$: Observable<string>, group = 'digitiser') {
    return combineLatest([
      this.getKeycloakUsersForGroup(group),
      search$.pipe(startWith(''))
    ]).pipe(
      map(([users, search]) => this.filterUsers(users, search))
    );
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
