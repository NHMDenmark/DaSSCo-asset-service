import {Injectable} from '@angular/core';
import {BehaviorSubject, map, Observable, ReplaySubject} from "rxjs";
import {OidcSecurityService} from "angular-auth-oidc-client";

@Injectable({providedIn: 'root'})
export class AuthService {
  private readonly checkAuthCompleteSubject = new BehaviorSubject<boolean>(false);
  readonly checkAuthComplete$ = this.checkAuthCompleteSubject.asObservable();
  readonly isAuthenticated$: Observable<boolean> =
    this.oidcSecurityService.isAuthenticated$
      .pipe(
        map(authenticationResult => authenticationResult.isAuthenticated)
      );
  loginInitialized: ReplaySubject<boolean | undefined> = new ReplaySubject<boolean | undefined>(1);

  constructor(private oidcSecurityService: OidcSecurityService) {
  }

  setCheckAuthComplete(): void {
    this.checkAuthCompleteSubject.next(true);
  }

  public login(): void {
    this.oidcSecurityService.authorize();
  }

  username$: Observable<string | undefined>
    = this.oidcSecurityService.userData$.pipe(
      map(data => data.userData.preferred_username)
    )

}

