import { Component} from '@angular/core';
import {InternalStatusService} from '../../services/internal-status.service';
import {filter, map, Observable} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {MatTableDataSource} from "@angular/material/table";
import {InternalStatusDataSource} from "../../types";
import {HttpStatusCode} from "@angular/common/http";

@Component({
  selector: 'dassco-status-widget',
  templateUrl: './status-widget.component.html',
  styleUrls: ['./status-widget.component.scss']
})
export class StatusWidgetComponent {
  today = new Date();
  displayedColumns: string[] = ['status', 'no'];
  dataSource = new MatTableDataSource<InternalStatusDataSource>();

  internalStatuses$: Observable<InternalStatusDataSource[]>
  = this.internalStatusService.internalStatuses$
    .pipe(
      filter(isNotUndefined),
      map(statuses => {
        const listData: InternalStatusDataSource[] = [];
        this.dataSource.data = listData;

        if (statuses.status === HttpStatusCode.NoContent || !statuses.body) {
          console.warn('No data received or data is null.');

          listData.push({status: 'COMPLETED', no: 0} as InternalStatusDataSource);
          listData.push({status: 'PENDING', no: 0} as InternalStatusDataSource);
        }

        const mapData = new Map(Object.entries(statuses.body));
        listData.push({status: 'COMPLETED', no: mapData.get('completed')} as InternalStatusDataSource);
        listData.push({status: 'PENDING', no: mapData.get('pending')} as InternalStatusDataSource);

        return listData;
      })
    );

  constructor(public internalStatusService: InternalStatusService) { }
}
