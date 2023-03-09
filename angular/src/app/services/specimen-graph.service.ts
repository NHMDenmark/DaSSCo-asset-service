import { Injectable } from '@angular/core';
import {OidcSecurityService} from "angular-auth-oidc-client";
import {HttpClient} from "@angular/common/http";
import {catchError, Observable, of, switchMap} from "rxjs";
import {SpecimenGraph} from "../types";
import {ChartOptions} from "chart.js";

@Injectable({
  providedIn: 'root'
})
export class SpecimenGraphService {
  baseUrl = '/api/v1/specimengraphinfo';

  constructor(
    public oidcSecurityService: OidcSecurityService
    , private http: HttpClient
  ) { }

  specimenData$: Observable<SpecimenGraph[] | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get<SpecimenGraph[]>(`${this.baseUrl}`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}`, undefined))
          );
      })
    );

  specimenPipeline$: Observable<SpecimenGraph[] | undefined>
    = this.oidcSecurityService.getAccessToken()
    .pipe(
      switchMap((token) => {
        return this.http.get<SpecimenGraph[]>(`${this.baseUrl}`, {headers: {'Authorization': 'Bearer ' + token}})
          .pipe(
            catchError(this.handleError(`get ${this.baseUrl}`, undefined))
          );
      })
    );

  getGraphOptions(yaxis: string, title: string): ChartOptions {
    return {
      responsive: true,
      maintainAspectRatio: true,
      aspectRatio: 2.5,
      skipNull: true,
      layout: {
        padding: 10
      },
      plugins: {
        title: {
          display: true,
          text: title,
          font: {
            size: 20
          },
          color: 'rgba(20, 48, 82, 0.9)'
        },
        legend: {
          position: 'top',
          labels: {
            sort(a, b, _data) {
              return a.text.split('_')[0].localeCompare(b.text.split('_')[0]);
            }
          }
        }
      },
      scales: {
        y: {
          title: {
            display: true,
            align: 'center',
            text: yaxis
          },
          ticks: {
            callback(val: number, _index: number) {
              return val % 1 === 0 ? val : '';
            }
          }
        }
        // x: {
        //   stacked: true
        // }
      }
    } as ChartOptions;
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(error);
      console.error(operation + ' - ' + JSON.stringify(error));
      return of(result as T);
    };
  }
}
