import {Component} from '@angular/core';
import {SpecimenGraphService} from '../../services/specimen-graph.service';
import {BehaviorSubject, combineLatest, filter, map, Observable} from 'rxjs';
import {defaultTimeFrame, GraphData, SpecimenGraph, StatValue, TimeFrame} from '../../types';
import {isNotNull, isNotUndefined} from '@northtech/ginnungagap';
import moment from 'moment/moment';
import {FormControl} from "@angular/forms";
import _default from "chart.js/dist/plugins/plugin.legend";

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
  timeFrameMap: Map<number, {period: string, amount: number, unit: string, format: string}> = new Map([
    [1, {period: 'WEEK', amount: 7, unit: 'days', format: 'DD-MMM-YY'}],
    [2, {period: 'MONTH', amount: 30, unit: 'days', format: 'DD-MMM-YY'}],
    [3, {period: 'YEAR', amount: 12, unit: 'months', format: 'MMM YY'}],
    [4, {period: 'MULTIYEAR', amount: 12, unit: 'months', format: 'MMM YY'}]
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
        const map = new Map<string, Map<string, number>>();
        specimens.forEach(s => {
          const key = this.getKey(s, statValue);
          if (now.diff(moment(s.createdDate), timeFrame.unit) <= timeFrame.amount) { // if it's within the timeframe
            const created = moment(s.createdDate).format(timeFrame.format);
            if (map.has(key)) { // if it exists
              const inst = map.get(key);
              if (inst) {
                if (inst.has(created)) { // if its value has been set, we up the total
                  const total = inst.get(created);
                  if (total) inst.set(created, total + 1); // splitting them up to satisfy typescripts need for undefined-validation sigh
                } else { // otherwise, we set the total to 1
                  inst.set(created, 1);
                }
              }
            } else { // if nothing, we create a new with the institute name
              map.set(key, new Map<string, number>([[created, 1]]));
            }
          }
        });
        const graphData: GraphData = {labels: this.createLabels(timeFrame), timeFrame: timeFrame, multi: false};
        if (timeFrame.period.includes('YEAR')) {
          if (timeFrame.period.includes('MULTI')) {
            graphData.subChart = map; // sets the subchart as the otherwise mainchart, as year mainchart will be with manipulated data
            graphData.multi = true;
          }
          map.forEach((totalPrDate, key, originalMap) => {
            const sortByDate = new Map(Array.from(totalPrDate).sort(([a], [b]) => a.localeCompare(b)));
            const addedList = new Map(Array.from(sortByDate)
              .map((curr, i, arr) => {
                if (arr[i - 1]) curr[1] += arr[i - 1][1];
                return curr;
              })
            );
            originalMap.set(key, addedList);
          });
          graphData.mainChart = map;
        } else {
          graphData.mainChart = map;
        }
        return graphData;
      })
    );

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
