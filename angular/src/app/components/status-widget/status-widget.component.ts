import { Component} from '@angular/core';
import {InternalStatusService} from '../../services/internal-status.service';
import {filter, map, Observable} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {MatTableDataSource} from "@angular/material/table";
import {InternalStatusDataSource} from "../../types";

@Component({
  selector: 'dassco-status-widget',
  templateUrl: './status-widget.component.html',
  styleUrls: ['./status-widget.component.scss']
})
export class StatusWidgetComponent {
  today = new Date();
  displayedColumns: string[] = ['status', 'no'];
  dataSource = new MatTableDataSource<InternalStatusDataSource>();

  internalStatuses$: Observable<Map<string, string>>
  = this.internalStatusService.internalStatuses$
    .pipe(
      filter(isNotUndefined),
      map(statuses => {
        const mapData = new Map(Object.entries(statuses.body));
        console.log(mapData)
        const listData = [];
        listData.push({status: 'COMPLETED', no: mapData.get('COMPLETED')} as InternalStatusDataSource);
        listData.push({status: 'ASSET_RECEIVED', no: mapData.get('ASSET_RECEIVED')} as InternalStatusDataSource);
        listData.push({status: 'METADATA_RECEIVED', no: mapData.get('METADATA_RECEIVED')} as InternalStatusDataSource);

        this.dataSource.data = listData;
        return statuses.body;
      })
    );

  constructor(public internalStatusService: InternalStatusService) { }
}
