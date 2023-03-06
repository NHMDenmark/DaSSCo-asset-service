import { Component } from '@angular/core';
import {SpecimenGraphService} from '../../../services/specimen-graph.service';
import {BehaviorSubject, combineLatest, filter, map, Observable} from 'rxjs';
import {TimeFrame, defaultTimeFrame} from '../../../types';
import {isNotUndefined} from '@northtech/ginnungagap';
import moment from 'moment/moment';

@Component({
  selector: 'dassco-specimen-institute',
  templateUrl: './specimen-institute.component.html',
  styleUrls: ['./specimen-institute.component.scss']
})
export class SpecimenInstituteComponent {
  chart: any;
  timeFrameSubject = new BehaviorSubject<TimeFrame>(defaultTimeFrame);

  graphInfo$: Observable<Map<string, Map<string, number>>>
    = combineLatest([
    this.specimenGraphService.specimenInstitution$.pipe(filter(isNotUndefined)),
    this.timeFrameSubject
  ])
    .pipe(
      map(([specimens, timeFrame]) => {
        const now = moment();
        const map = new Map<string, Map<string, number>>();
        specimens.forEach(s => {
          if (now.diff(moment(s.createdDate), timeFrame.unit) <= timeFrame.amount) { // if it's within the timeframe
            const created = moment(s.createdDate).format(timeFrame.format);
            if (map.has(s.instituteName)) { // if it exists
              const inst = map.get(s.instituteName);
              if (inst) {
                if (inst.has(created)) { // if its value has been set, we up the total
                  const total = inst.get(created);
                  if (total) inst.set(created, total + 1); // splitting them up to satisfy typescripts need for undefined-validation sigh
                } else { // otherwise, we set the total to 1
                  inst.set(created, 1);
                }
              }
            } else { // if nothing, we create a new with the institute name
              map.set(s.instituteName, new Map<string, number>([[created, 1]]));
            }
          }
        });
        if (timeFrame.period === 'YEAR') {
          map.forEach((totalPrDate, key, originalMap) => {
            const sortByDate = new Map(Array.from(totalPrDate).sort(([a], [b]) => a.localeCompare(b)));
            const test = new Map(Array.from(sortByDate)
              .map((curr, i, arr) => {
                if (arr[i - 1]) curr[1] += arr[i - 1][1];
                return curr;
              })
            );
            originalMap.set(key, test);
          });
        }
        return map;
      })
    );

  labels$
    = this.timeFrameSubject
    .pipe(
      map((timeFrame) => this.createLabels(timeFrame))
    );

  constructor(
    public specimenGraphService: SpecimenGraphService
  ) { }

  createLabels(timeFrame: TimeFrame): string[] {
    const labels: string[] = [];
      for (let i = timeFrame.amount - 1; i >= 0; i--) {
        labels.push(moment().subtract(i, timeFrame.unit).format(timeFrame.format));
      }
    return labels;
  }

  setTimeFrame(timeFrame: TimeFrame) {
    this.timeFrameSubject.next(timeFrame);
  }
}
