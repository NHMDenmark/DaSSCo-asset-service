import {Component} from '@angular/core';
import {OidcSecurityService} from "angular-auth-oidc-client";
import {AuthService} from "./services/auth.service";
import {Router} from "@angular/router";
import {ReplaySubject} from "rxjs";

@Component({
  selector: 'dassco-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {

  activeMenu: ReplaySubject<string | undefined> = new ReplaySubject<string | undefined>(1);

  constructor(private oidcSecurityService: OidcSecurityService,
              public authService: AuthService,
              private router: Router
  ) {
    this.setActiveRoute();
  }

  ngOnInit(): void {
    this.oidcSecurityService.checkAuth().subscribe({
      next: loginResponse => {
        const redirect = sessionStorage.getItem('postLoginUrl');
        sessionStorage.removeItem('postLoginUrl');
        this.authService.loginInitialized.next(redirect == null ? true : undefined)
        /*let roles: string[];
        try {
          roles = jwtDecode<KeycloakRolesTokenStub>(loginResponse.accessToken).realm_access?.roles ?? [];
        } catch (err) {
          roles = [];
        }

        this.appState.set({
          loginInitialized: redirect == null ? true : undefined,
          isClimbAlongAdmin: roles.includes('ClimbAlongAdmin'),
          isClimbAlongUser: roles.includes('ClimbAlongUser'),
          oidcUsername: loginResponse.userData?.preferred_username,
          oidcEmail: loginResponse.userData?.email,
          oidcSub: loginResponse.userData?.sub
        });*/

        this.authService.setCheckAuthComplete();

        if (redirect && loginResponse.isAuthenticated) {
          document.location = redirect;
        }
      },
      error: err => {
        console.log(err)
      }
    });
  }

  setActiveRoute(): void {
    let path: string | undefined = document.location.pathname.split('/')[1];
    switch (path) {
      case 'assets':
      case 'graphs':
      case 'statistics':
      case 'docs':
        this.activeMenu.next(path);
        break;
      default:
        this.activeMenu.next('statistics');
        break;
    }
  }

  navigateToSite(location: string): void {
    this.router.navigate(['/' + location]).then(r =>{
      if (r) {
        this.activeMenu.next(location)
        history.replaceState({}, '', '/' + location)
      }
    })
  }
}
