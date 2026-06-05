import {Component, inject} from '@angular/core';
import {AuthService} from './services/auth.service';
import {Router} from '@angular/router';
import {ReplaySubject} from 'rxjs';
import {DasscoHomepage} from './utility';

@Component({
  selector: 'dassco-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  activeMenu: ReplaySubject<string | undefined> = new ReplaySubject<string | undefined>(1);
  authService = inject(AuthService);
  private router = inject(Router);
  dasscoHomepage = inject(DasscoHomepage);

  constructor() {
    this.setActiveRoute();
  }

  setActiveRoute(): void {
    const path: string | undefined = document.location.pathname.split('/')[2];
    switch (path) {
      case 'assets':
      case 'graphs':
      case 'statistics':
      case 'queries':
      case 'docs':
        this.activeMenu.next(path);
        break;
      default:
        this.activeMenu.next('statistics');
        break;
    }
  }

  navigateToSite(location: string): void {
    this.router.navigate([location]).then((r) => {
      if (r) {
        this.activeMenu.next(location);
        history.replaceState({}, '', location);
      }
    });
  }
}
