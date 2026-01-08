import {Component, AfterViewChecked} from '@angular/core';
import {InternalStatusService} from '../../services/internal-status.service';
import {filter, map, Observable, combineLatest, switchMap, debounceTime} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {MatTableDataSource} from '@angular/material/table';
import {InternalStatusDataSource} from '../../types/graph-types';
import {HttpResponse} from '@angular/common/http';
import {SpecimenGraphService} from '../../services/specimen-graph.service';
import {animate, state, style, transition, trigger} from '@angular/animations';

@Component({
  selector: 'dassco-status-widget',
  templateUrl: './status-widget.component.html',
  styleUrls: ['./status-widget.component.scss'],
  animations: [
    trigger('expandCollapse', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('150ms cubic-bezier(0.4, 0.0, 0.2, 1)'))
    ])
  ]
})
export class StatusWidgetComponent implements AfterViewChecked {
  expanded = false;
  private shouldScroll = false;

  startDate = this.specimenGraphService.statisticsStartDate;
  endDate = this.specimenGraphService.statisticsEndDate;

  today = new Date();
  displayedColumns: string[] = ['status', 'no'];

  /*dailyStatus$: Observable<MatTableDataSource<InternalStatusDataSource>>
  = this.internalStatusService.dailyInternalStatuses$
    .pipe(filter(isNotUndefined))
    .pipe(
      map((status: HttpResponse<InternalStatusDataSource>) => {
        let dailyStatuses = new MatTableDataSource<InternalStatusDataSource>();
        return this.getStatusFromResponse(status, dailyStatuses);
      })
    )*/

  dailyStatus$: Observable<MatTableDataSource<InternalStatusDataSource>> = combineLatest([
    this.startDate,
    this.endDate
  ]).pipe(
    debounceTime(250),
    filter(([startDate, endDate]) => startDate !== undefined && endDate !== undefined),
    switchMap(([startDate, endDate]) => {
      return this.internalStatusService
        .customRangeInterStatuses(startDate, endDate)
        .pipe(filter(isNotUndefined))
        .pipe(
          map((status: HttpResponse<InternalStatusDataSource>) => {
            let dailyStatuses = new MatTableDataSource<InternalStatusDataSource>();
            return this.getStatusFromResponse(status, dailyStatuses);
          })
        );
    })
  );

  totalStatus$: Observable<MatTableDataSource<InternalStatusDataSource>> =
    this.internalStatusService.totalInternalStatuses$.pipe(filter(isNotUndefined)).pipe(
      map((status: HttpResponse<InternalStatusDataSource>) => {
        let totalStatuses = new MatTableDataSource<InternalStatusDataSource>();
        return this.getStatusFromResponse(status, totalStatuses);
      })
    );

  getStatusFromResponse(
    response: HttpResponse<InternalStatusDataSource>,
    dataSource: MatTableDataSource<InternalStatusDataSource>
  ): MatTableDataSource<InternalStatusDataSource> {
    if (response.ok && response.body) {
      const statusMap = new Map(Object.entries(response.body));
      statusMap.forEach((value: number, key: string) => {
        dataSource.data.push(<InternalStatusDataSource>{status: key, no: value});
      });
    } else {
      dataSource.data = <InternalStatusDataSource[]>[
        {status: 'COMPLETED', no: 0},
        {status: 'PENDING', no: 0},
        {status: 'FAILED', no: 0}
      ];
    }
    return dataSource;
  }

  toggleExpanded() {
    if (!this.expanded) {
      this.expanded = true;
      // Wait for animation to complete (150ms) then scroll
      setTimeout(() => {
        this.shouldScroll = true;
      }, 180);
    } else {
      this.expanded = false;
    }
  }

  ngAfterViewChecked() {
    if (this.shouldScroll) {
      // Scroll to the bottom of the page
      window.scrollTo({
        top: document.documentElement.scrollHeight,
        behavior: 'smooth'
      });
      this.shouldScroll = false;
    }
  }

  constructor(public internalStatusService: InternalStatusService, public specimenGraphService: SpecimenGraphService) {}
}
