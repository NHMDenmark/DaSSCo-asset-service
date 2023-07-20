import { Component} from '@angular/core';
import {InternalStatusService} from '../../services/internal-status.service';
import {combineLatest, filter, map, Observable} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {MatTableDataSource} from '@angular/material/table';
import {InternalStatusDataSource} from '../../types';
import {HttpStatusCode} from '@angular/common/http';

@Component({
  selector: 'dassco-status-widget',
  templateUrl: './status-widget.component.html',
  styleUrls: ['./status-widget.component.scss']
})
export class StatusWidgetComponent {
  today = new Date();
  displayedColumns: string[] = ['status', 'no'];
  dailyDataSource = new MatTableDataSource<InternalStatusDataSource>();
  totalDataSource = new MatTableDataSource<InternalStatusDataSource>();

  dailyInternalStatuses$: Observable<InternalStatusDataSource[]>
  = combineLatest([
    this.internalStatusService.dailyInternalStatuses$.pipe(filter(isNotUndefined)),
    this.internalStatusService.totalInternalStatuses$.pipe(filter(isNotUndefined))
  ])
    .pipe(
      map(([dailyStatuses, totalStatuses]) => {
        const dailyListData: InternalStatusDataSource[] = [];
        const totalListData: InternalStatusDataSource[] = [];
        this.dailyDataSource.data = dailyListData;
        this.totalDataSource.data = totalListData;

        // todo please don't look at this code by god i just needed it to work for now okay please i swear i'll change it

        if (dailyStatuses.status === HttpStatusCode.NoContent || !dailyStatuses.body) {
          console.warn('No data received or data is null.');

          dailyListData.push({status: 'COMPLETED', no: 0} as InternalStatusDataSource);
          dailyListData.push({status: 'PENDING', no: 0} as InternalStatusDataSource);
          dailyListData.push({status: 'FAILED', no: 0} as InternalStatusDataSource);

          totalListData.push({status: 'COMPLETED', no: 0} as InternalStatusDataSource);
          totalListData.push({status: 'PENDING', no: 0} as InternalStatusDataSource);
          totalListData.push({status: 'FAILED', no: 0} as InternalStatusDataSource);
        }

        const mapData = new Map(Object.entries(dailyStatuses.body));
        dailyListData.push({status: 'COMPLETED', no: mapData.get('completed')} as InternalStatusDataSource);
        dailyListData.push({status: 'PENDING', no: mapData.get('pending')} as InternalStatusDataSource);
        dailyListData.push({status: 'FAILED', no: mapData.get('failed')} as InternalStatusDataSource);

        const mapDataTotal = new Map(Object.entries(totalStatuses.body));
        totalListData.push({status: 'COMPLETED', no: mapDataTotal.get('completed')} as InternalStatusDataSource);
        totalListData.push({status: 'PENDING', no: mapDataTotal.get('pending')} as InternalStatusDataSource);
        totalListData.push({status: 'FAILED', no: mapDataTotal.get('failed')} as InternalStatusDataSource);

        return dailyListData;
      })
    );

  constructor(public internalStatusService: InternalStatusService) { }
}
