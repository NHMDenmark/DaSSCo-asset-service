import { Component} from '@angular/core';
import {InternalStatusService} from '../../services/internal-status.service';
import {filter, map, Observable} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';

@Component({
  selector: 'dassco-status-widget',
  templateUrl: './status-widget.component.html',
  styleUrls: ['./status-widget.component.scss']
})
export class StatusWidgetComponent {
  today = new Date();

  internalStatuses$: Observable<Map<string, string>>
  = this.internalStatusService.internalStatuses$
    .pipe(
      filter(isNotUndefined),
      map(statuses => {
        return new Map(Object.entries(statuses.body));
      })
    );

  constructor(public internalStatusService: InternalStatusService) { }
}
