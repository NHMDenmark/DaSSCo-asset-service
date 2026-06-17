import {Component, inject} from '@angular/core';
import {AuthService} from './services/auth.service';
import {NavigationEnd, Router} from '@angular/router';
import {filter, ReplaySubject} from 'rxjs';
import {DasscoHomepage, WikiPageUrl} from './utility';

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
  wikiPageUrl = inject(WikiPageUrl);

  constructor() {
    this.setActiveRoute(document.location.pathname);
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.setActiveRoute(event.urlAfterRedirects));
  }

  setActiveRoute(urlOrPath: string = document.location.pathname): void {
    const path = this.getRouteSegments(urlOrPath)[0];
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

  login(): void {
    const assetGuid = this.getExternalDetailedViewAssetGuid(document.location.pathname);
    if (assetGuid) {
      sessionStorage.setItem('postLoginUrl', `/detailed-view/${assetGuid}`);
    }
    this.authService.login();
  }

  navigateToSite(location: string): void {
    this.router.navigate([location]).then((r) => {
      if (r) {
        this.activeMenu.next(location);
        history.replaceState({}, '', location);
      }
    });
  }

  private getExternalDetailedViewAssetGuid(urlOrPath: string): string | undefined {
    const segments = this.getRouteSegments(urlOrPath);
    if (segments[0] !== 'extern' || segments[1] !== 'detailed-view') {
      return undefined;
    }
    return segments[2];
  }

  private getRouteSegments(urlOrPath: string): string[] {
    const pathname = urlOrPath.split(/[?#]/)[0];
    const segments = pathname.split('/').filter(Boolean);
    if (segments[0] === 'ars') {
      return segments.slice(1);
    }
    return segments;
  }
}
