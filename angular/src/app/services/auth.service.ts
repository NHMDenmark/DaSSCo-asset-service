import {Injectable} from '@angular/core';
import {BehaviorSubject, combineLatest, filter, firstValueFrom, map, Observable, ReplaySubject, switchMap} from 'rxjs';
import {OidcSecurityService} from 'angular-auth-oidc-client';

@Injectable({providedIn: 'root'})
export class AuthService {
  private readonly checkAuthCompleteSubject = new BehaviorSubject<boolean>(false);
  readonly checkAuthComplete$ = this.checkAuthCompleteSubject.asObservable();
  readonly isAuthenticated$: Observable<boolean> = this.oidcSecurityService.isAuthenticated$.pipe(
    map((authenticationResult) => authenticationResult.isAuthenticated)
  );
  loginInitialized: ReplaySubject<boolean | undefined> = new ReplaySubject<boolean | undefined>(1);

  constructor(private oidcSecurityService: OidcSecurityService) {}

  async initializeAuthentication(): Promise<void> {
    try {
      const loginResponse = await firstValueFrom(this.oidcSecurityService.checkAuth());
      const redirect = sessionStorage.getItem('postLoginUrl');
      sessionStorage.removeItem('postLoginUrl');
      this.loginInitialized.next(redirect == null ? true : undefined);
      this.setCheckAuthComplete();

      if (redirect && loginResponse.isAuthenticated) {
        document.location = '/ars' + redirect;
      }
    } catch (error) {
      console.log(error);
      this.loginInitialized.next(true);
      this.setCheckAuthComplete();
    }
  }

  setCheckAuthComplete(): void {
    this.checkAuthCompleteSubject.next(true);
  }

  login(): void {
    this.oidcSecurityService.authorize();
  }
  logout(): void {
    this.oidcSecurityService.logoff();
  }

  getAccessToken(): Observable<string> {
    return combineLatest([this.isAuthenticated$, this.checkAuthComplete$]).pipe(
      filter(([isAuthenticated, checkAuthComplete]) => isAuthenticated && checkAuthComplete),
      switchMap(() => this.oidcSecurityService.getAccessToken()),
      filter((token): token is string => !!token)
    );
  }

  username$: Observable<string | undefined> = this.oidcSecurityService.userData$.pipe(
    map((data) => data.userData.preferred_username)
  );
}
