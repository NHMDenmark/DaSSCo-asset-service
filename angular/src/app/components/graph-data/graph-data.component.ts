import {Component} from '@angular/core';
import {SpecimenGraphService} from '../../services/specimen-graph.service';
import {BehaviorSubject, combineLatest, filter, map, Observable, pairwise, startWith} from 'rxjs';
import {defaultTimeFrame, GraphData, MY_FORMATS, SpecimenGraph, StatValue, TimeFrame} from '../../types';
import {isNotNull, isNotUndefined} from '@northtech/ginnungagap';
import moment, {Moment} from 'moment/moment';
import {FormControl, FormGroup} from '@angular/forms';
import {MAT_DATE_FORMATS} from "@angular/material/core";

@Component({
  selector: 'dassco-specimen-institute',
  templateUrl: './graph-data.component.html',
  styleUrls: ['./graph-data.component.scss'],
  providers: [
    { provide: MAT_DATE_FORMATS, useValue: MY_FORMATS }
  ]
})
export class GraphDataComponent {
  chart: any;
  timeFrameMap: Map<number, TimeFrame> = new Map([
    [1, {period: 'WEEK', unit: 'days', format: 'DD-MMM-YY', startDate: moment().subtract(7, 'days'), endDate: moment()}],
    [2, {period: 'MONTH', unit: 'days', format: 'DD-MMM-YY', startDate: moment().subtract(1, 'months'), endDate: moment()}],
    [3, {period: 'YEAR', unit: 'months', format: 'MMM-YY', startDate: moment().subtract(12, 'months'), endDate: moment()}],
    [4, {period: 'COMBINEDTOTAL', unit: 'months', format: 'MMM-YY', startDate: moment().subtract(12, 'months'), endDate: moment()}]
  ]);
  timeFrameSubject = new BehaviorSubject<TimeFrame>(this.timeFrameMap.get(defaultTimeFrame) as TimeFrame);
  statValueSubject = new BehaviorSubject<StatValue>(StatValue.INSTITUTE);
  title = 'Specimens / Institute';
  timeFrameForm = new FormControl(defaultTimeFrame);
  statForm = new FormControl(0);
  timeframeRange = new FormGroup({
    start: new FormControl<Moment | null>(null, {updateOn: 'blur'}),
    end: new FormControl<Moment | null>(null, {updateOn: 'blur'})
  });

  graphInfo$: Observable<GraphData>
    = combineLatest([
    this.specimenGraphService.specimenData$.pipe(filter(isNotUndefined)),
    this.timeFrameSubject,
    this.statValueSubject
  ])
    .pipe(
      map(([specimens, timeFrame, statValue]) => {
        const main = new Map<string, Map<string, number>>();
        const graphData: GraphData = {labels: this.createLabels(timeFrame), timeFrame: timeFrame, multi: false};

        specimens.forEach(s => {
          const key = this.getKey(s, statValue); // institute, pipeline, etc...
          if (moment(s.createdDate).isBetween(timeFrame.startDate, timeFrame.endDate, timeFrame.unit, '[]')) {
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
        // todod if main is empty, there's no data.
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
    combineLatest([
      this.timeFrameForm.valueChanges.pipe(
        startWith(defaultTimeFrame, defaultTimeFrame),
        filter(isNotNull),
        pairwise()
      ),
      this.timeframeRange.valueChanges.pipe(
        startWith(null)
      )
    ])
      .subscribe(([[prevForm, currForm], range]) => {
        const prevTf = this.timeFrameMap.get(prevForm) as TimeFrame;
        const nextTf = this.timeFrameMap.get(currForm) as TimeFrame;

        if (range?.start && range.end) { // if there's custom range
          if (prevTf.unit !== nextTf.unit) { // if it changes from daily to yearly view or vice versa
            this.clearCustomTimeFrame();
            this.timeFrameSubject.next(nextTf);
            this.timeFrameForm.setValue(currForm); // todo make this prettier when you have time.....
          } else if (moment(range.start, 'DD-MM-YYYY ', true).isValid()
            && moment(range.end, 'DD-MM-YYYY ', true).isValid()) {
            this.timeFrameSubject.next({
              period: nextTf.period,
              unit: nextTf.unit,
              format: nextTf.format,
              startDate: range.start,
              endDate: range.end
            });
          }
        } else {
          this.timeFrameSubject.next(nextTf);
        }
      });

    // this.timeFrameForm.valueChanges
    //   .pipe(
    //     filter(isNotNull),
    //     startWith(defaultTimeFrame),
    //     pairwise()
    //   )
    //   .subscribe(([prev, next]) => {
    //     const prevTf = this.timeFrameMap.get(prev) as TimeFrame;
    //     console.log(prevTf)
    //     const nextTf = this.timeFrameMap.get(next) as TimeFrame;
    //     this.timeFrameSubject.next(nextTf);
    //     // if (prevTf.unit !== nextTf.unit) this.clearCustomTimeFrame();
    //   });

    this.statForm.valueChanges.pipe(filter(isNotNull))
      .subscribe(val => this.setStatValue(val));

    // this.timeframeRange.valueChanges.pipe(filter(isNotNull))
    //   .subscribe(val => {
    //     if (val.start && val.end) {
    //       if (moment(val.start, 'DD-MM-YYYY ', true).isValid()
    //         && moment(val.end, 'DD-MM-YYYY ', true).isValid()) {
    //         if (this.timeFrameForm.value) {
    //           const currTimeFrame = this.timeFrameMap.get(this.timeFrameForm.value) as TimeFrame;
    //           this.timeFrameSubject.next({period: currTimeFrame.period, unit: currTimeFrame.unit, format: currTimeFrame.format, startDate: val.start, endDate: val.end});
    //         }
    //       }
    //     }
    //   });
  }

  createLabels(timeFrame: TimeFrame): string[] {
    const labels: string[] = [];
    const duration = timeFrame.endDate.clone().diff(timeFrame.startDate.clone(), timeFrame.unit);
    for (let i = duration; i >= 0; i--) {
      labels.push(timeFrame.endDate.clone().subtract(i, timeFrame.unit).format(timeFrame.format));
    }
    return labels;
  }

  setStatValue(statValue: StatValue) {
    this.statValueSubject.next(statValue);
    if (statValue === StatValue.INSTITUTE) this.title = 'Specimens / Institute';
    if (statValue === StatValue.PIPELINE) this.title = 'Specimens / Pipeline';
    if (statValue === StatValue.WORKSTATION) this.title = 'Specimens / Workstation';
  }

  clearCustomTimeFrame() {
    if (this.timeFrameForm.value) {
      const originalTf = this.timeFrameMap.get(this.timeFrameForm.value);
      this.timeFrameSubject.next(originalTf as TimeFrame);
      this.timeframeRange.reset();
    }
  }
}
