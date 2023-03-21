import {Component} from '@angular/core';
import {SpecimenGraphService} from '../../services/specimen-graph.service';
import {BehaviorSubject, combineLatest, filter, map, Observable} from 'rxjs';
import {defaultTimeFrame, GraphData, SpecimenGraph, StatValue, TimeFrame} from '../../types';
import {isNotNull, isNotUndefined} from '@northtech/ginnungagap';
import moment from 'moment/moment';
import {FormControl, FormGroup} from '@angular/forms';

@Component({
  selector: 'dassco-specimen-institute',
  templateUrl: './graph-data.component.html',
  styleUrls: ['./graph-data.component.scss']
})
export class GraphDataComponent {
  chart: any;
  timeFrameSubject = new BehaviorSubject<TimeFrame>(defaultTimeFrame);
  statValueSubject = new BehaviorSubject<StatValue>(StatValue.INSTITUTE);
  title = 'Specimens / Institute';
  timeFrameForm = new FormControl(1);
  statForm = new FormControl(0);
  timeFrameRange = new FormGroup({
    start: new FormControl<Date | null>(null),
    end: new FormControl<Date | null>(null)
  });
  timeFrameMap: Map<number, {period: string, amount: number, unit: string, format: string}> = new Map([
    [1, {period: 'WEEK', amount: 7, unit: 'days', format: 'DD-MMM-YY'}],
    [2, {period: 'MONTH', amount: 30, unit: 'days', format: 'DD-MMM-YY'}],
    [3, {period: 'YEAR', amount: 12, unit: 'months', format: 'MMM-YY'}],
    [4, {period: 'COMBINEDTOTAL', amount: 12, unit: 'months', format: 'MMM-YY'}]
  ]);

  graphInfo$: Observable<GraphData>
    = combineLatest([
    this.specimenGraphService.specimenData$.pipe(filter(isNotUndefined)),
    this.timeFrameSubject,
    this.statValueSubject
  ])
    .pipe(
      map(([specimens, timeFrame, statValue]) => {
        const now = moment();
        const main = new Map<string, Map<string, number>>();
        const graphData: GraphData = {labels: this.createLabels(timeFrame), timeFrame: timeFrame, multi: false};

        specimens.forEach(s => {
          const key = this.getKey(s, statValue); // institute, pipeline, etc...
          if (now.diff(moment(s.createdDate), timeFrame.unit) <= timeFrame.amount) { // if it's within the timeframe
            const created = moment(s.createdDate).format(timeFrame.format);
            if (main.has(key)) { // if it already exists
              const inst = main.get(key);
              if (inst) {
                this.setTotalFromKey(inst, created, 1);
              }
            } else { // if nothing, we create a new with the institute name
              main.set(key, new Map<string, number>([[created, 1]]));
            }
          }
        });
        graphData.mainChart = main;

        if (timeFrame.period.includes('YEAR')) {
          main.forEach((totalPrDate, key, originalMap) => {
            originalMap.set(key, this.getAccumulativeTotal(totalPrDate, timeFrame));
          });
        }

        if (timeFrame.period.includes('COMBINEDTOTAL')) {
          graphData.multi = true;
          graphData.subChart = main; // sets the subchart as the (normally) mainchart, as COMBINED needs a manipulated linechart (aka. main)
          const combinedTotal = new Map<string, number>();

          main.forEach((totalPrDate, _inst, _originalMap) => {
            totalPrDate.forEach((total, date, _originalMap) => {
              this.setTotalFromKey(combinedTotal, date, total);
            });
          });
          graphData.mainChart = new Map<string, Map<string, number>>([['Total', this.getAccumulativeTotal(combinedTotal, timeFrame)]]);
        }
        return graphData;
      })
    );

  setTotalFromKey(map: Map<string, number>, hasKey: string, base: number) {
    if (map.has(hasKey)) { // if its value has been set, we up the total
      const total = map.get(hasKey);
      if (total) map.set(hasKey, total + base); // splitting them up to satisfy typescripts need for undefined-validation sigh
    } else { // otherwise, we set the total to 1
      map.set(hasKey, base);
    }
  }

  getAccumulativeTotal(map: Map<string, number>, timeFrame: TimeFrame): Map<string, number> { // sorts for dates and then adds the totals
    return new Map([...map.entries()]
      .sort(([a], [b]) => moment(a, timeFrame.format).isBefore(moment(b, timeFrame.format)) ? -1 : 1)
      .map((curr, i, arr) => {
        if (arr[i - 1]) curr[1] += arr[i - 1][1];
        return curr;
      })
    );
  }

  getKey(specimen: SpecimenGraph, statValue: StatValue): string {
    if (statValue === StatValue.INSTITUTE) return specimen.instituteName;
    if (statValue === StatValue.PIPELINE) return specimen.pipelineName;
    return specimen.workstationName;
  }

  constructor(public specimenGraphService: SpecimenGraphService) {
    this.timeFrameForm.valueChanges.pipe(filter(isNotNull))
      .subscribe(val => {
        this.timeFrameSubject.next(this.timeFrameMap.get(val) as TimeFrame)
      });

    this.statForm.valueChanges.pipe(filter(isNotNull))
      .subscribe(val => this.setStatValue(val));
  }

  createLabels(timeFrame: TimeFrame): string[] {
    const labels: string[] = [];
      for (let i = timeFrame.amount - 1; i >= 0; i--) {
        labels.push(moment().subtract(i, timeFrame.unit).format(timeFrame.format));
      }
    return labels;
  }

  setStatValue(statValue: StatValue) {
    this.statValueSubject.next(statValue);
    if (statValue === StatValue.INSTITUTE) this.title = 'Specimens / Institute';
    if (statValue === StatValue.PIPELINE) this.title = 'Specimens / Pipeline';
    if (statValue === StatValue.WORKSTATION) this.title = 'Specimens / Workstation';
  }
}
