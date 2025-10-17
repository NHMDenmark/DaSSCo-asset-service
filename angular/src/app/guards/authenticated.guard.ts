import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {combineLatest, map, Observable} from 'rxjs';
import {AuthService} from "../services/auth.service";


@Injectable({
  providedIn: 'root'
})
export class AuthenticatedGuard implements CanActivate {
  readonly authenticated$ = this.authService.isAuthenticated$;
  readonly checkAuthComplete$ = this.authService.checkAuthComplete$;

  constructor(private authService: AuthService, private router: Router) {
  }

  canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> | boolean {
    return combineLatest([this.authenticated$, this.checkAuthComplete$]).pipe(
      map(([authenticated, checkAuthComplete]) => {
        if(checkAuthComplete) {
          if (!authenticated) {
            sessionStorage.setItem('postLoginUrl', state.url);
            this.router.dispose();
            this.authService.login();
            return false;
          } else {
            return true;
          }
        }
        return false;
      })
  );
  }
}
