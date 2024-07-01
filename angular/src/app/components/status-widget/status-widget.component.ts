import { Component} from '@angular/core';
import {InternalStatusService} from '../../services/internal-status.service';
import {filter, map, Observable} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {MatTableDataSource} from '@angular/material/table';
import {InternalStatusDataSource} from '../../types/types';
import {HttpResponse} from '@angular/common/http';

@Component({
  selector: 'dassco-status-widget',
  templateUrl: './status-widget.component.html',
  styleUrls: ['./status-widget.component.scss']
})
export class StatusWidgetComponent {
  today = new Date();
  displayedColumns: string[] = ['status', 'no'];

  dailyStatus$: Observable<MatTableDataSource<InternalStatusDataSource>>
  = this.internalStatusService.dailyInternalStatuses$
    .pipe(filter(isNotUndefined))
    .pipe(
      map((status: HttpResponse<InternalStatusDataSource>) => {
        let dailyStatuses = new MatTableDataSource<InternalStatusDataSource>();
        return this.getStatusFromResponse(status, dailyStatuses);
      })
    )

  totalStatus$: Observable<MatTableDataSource<InternalStatusDataSource>>
  = this.internalStatusService.totalInternalStatuses$
    .pipe(filter(isNotUndefined))
    .pipe(
      map((status: HttpResponse<InternalStatusDataSource>) => {
        let totalStatuses = new MatTableDataSource<InternalStatusDataSource>();
        return this.getStatusFromResponse(status, totalStatuses);
      })
    )

  getStatusFromResponse(response: HttpResponse<InternalStatusDataSource>, dataSource: MatTableDataSource<InternalStatusDataSource>): MatTableDataSource<InternalStatusDataSource> {
    if (response.ok && response.body) {
      const statusMap = new Map(Object.entries(response.body));
      statusMap.forEach((value: number, key: string) => {
        dataSource.data.push(<InternalStatusDataSource>{status: key, no: value})
      })
    } else {
      dataSource.data = <InternalStatusDataSource[]>[{status: 'COMPLETED', no: 0}, {status: 'PENDING', no: 0}, {status: 'FAILED', no: 0}];
    }
    return dataSource;
  }

  constructor(public internalStatusService: InternalStatusService) {}
}
