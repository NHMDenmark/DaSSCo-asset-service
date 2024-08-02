import {Component, OnDestroy, OnInit} from '@angular/core';
import {OidcSecurityService} from "angular-auth-oidc-client";
import {Subject, takeUntil} from "rxjs";

@Component({
  selector: 'dassco-docs',
  templateUrl: './docs.component.html',
  styleUrls: ['./docs.component.scss']
})
export class DocsComponent implements OnInit, OnDestroy {
  #componentDestroyed!: Subject<unknown>;
  constructor(private oidcSecurityService: OidcSecurityService) { }

  ngOnInit(): void {
    this.#componentDestroyed = new Subject<unknown>();
    let docEl = document.getElementById("rapidocs");
    this.oidcSecurityService.getAccessToken()
      .pipe(takeUntil(this.#componentDestroyed))
      .subscribe(token => {
        const headers = new Headers({
          'Authorization': 'Bearer ' + token
        });
        fetch('api/openapi.json', { headers: headers })
          .then(response => response.json())
          .then(data => {
            if (docEl) {
              (docEl as any).loadSpec(data);
              this.setTheme(docEl)
            }
          })
          .catch(error => {
            console.error(error);
          });
      });

    this.oidcSecurityService.getAccessToken()
      .pipe(takeUntil(this.#componentDestroyed))
      .subscribe(token => {
        (docEl as any).addEventListener('spec-loaded', (_e: any) => {
          (docEl as any).setApiKey('dassco-idp', 'Bearer ' + token);
        });
      });
  }

  setTheme(element: HTMLElement) {
    //Main site
    element.setAttribute('theme', 'light')
    element.setAttribute('bg-color', '#FFFCFF')
    element.setAttribute('text-color', '#22181C')
    element.setAttribute('regular-font', 'caviar')
    element.setAttribute('mono-font', 'geomatrix-medium')
    //Navigation Bar
    element.setAttribute('nav-bg-color', '#a7d5db')
    element.setAttribute('nav-hover-bg-color', '#fdfafd')
    element.setAttribute('nav-text-color', '#22181C')
    element.setAttribute('nav-accent-text-color', '#22181C')
    element.setAttribute('nav-accent-color', '#054791')
    element.setAttribute('nav-hover-text-color', '#22181C')
    element.setAttribute('primary-color', '#054791')
  }

  ngOnDestroy(): void {
    this.#componentDestroyed.next(true);
    this.#componentDestroyed.complete();
  }
}
