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
  selectedDoc : string;
  urls : { label: string, value: string }[] = [
    { label: "dassco-asset-service", value: "/api/openapi.json" },
    { label: "dassco-file-proxy", value: "http://localhost:8080/file_proxy/api/openapi.json"}
  ]
  constructor(private oidcSecurityService: OidcSecurityService) {
    this.selectedDoc = this.urls[0].value;
  }

  ngOnInit(): void {
    this.#componentDestroyed = new Subject<unknown>();
    this.loadData(this.selectedDoc);
  }

  loadData(url : string){
    let docEl = document.getElementById("rapidocs");
    this.oidcSecurityService.getAccessToken()
      .pipe(takeUntil(this.#componentDestroyed))
      .subscribe(token => {
        const headers = new Headers({
          'Authorization': 'Bearer ' + token
        })
        fetch(url, { headers: headers })
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

  onSelectChange() {
    this.loadData(this.selectedDoc);
  }
}
