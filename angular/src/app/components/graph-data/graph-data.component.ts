import {Component} from '@angular/core';
import {SpecimenGraphService} from '../../services/specimen-graph.service';
import {BehaviorSubject, filter, map, Observable, startWith} from 'rxjs';
import {
  defaultView,
  StatValue,
  GraphStatsV2, ViewV2, MY_FORMATS, ChartDataTypes
} from '../../types';
import {isNotNull, isNotUndefined} from '@northtech/ginnungagap';
import moment, {Moment} from 'moment-timezone';
import {FormControl, FormGroup} from '@angular/forms';
import {DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE} from '@angular/material/core';
import {MAT_MOMENT_DATE_ADAPTER_OPTIONS, MomentDateAdapter} from '@angular/material-moment-adapter';

@Component({
  selector: 'dassco-graph-data',
  templateUrl: './graph-data.component.html',
  styleUrls: ['./graph-data.component.scss'],
  providers: [
    { provide: DateAdapter, useClass: MomentDateAdapter, deps: [MAT_DATE_LOCALE,
      MAT_MOMENT_DATE_ADAPTER_OPTIONS] },
    { provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS, useValue: { useUtc: true } },
    {
      provide: MAT_DATE_FORMATS, useValue: MY_FORMATS
    }
  ]
})
export class GraphDataComponent {
  chart: any;
  viewForm = new FormControl(defaultView);
  statValueSubject = new BehaviorSubject<StatValue>(StatValue.INSTITUTE);
  statsV2Subject = new BehaviorSubject<Map<string, Map<string, GraphStatsV2>> | undefined>(undefined); // incremental: {dato, data}, exponential: {dato, data}
  statsV2$ = this.statsV2Subject.asObservable();
  title = 'Specimens / Institute';
  statValue = StatValue.INSTITUTE; // the statistics chosen -> institute, pipeline, workstation
  statForm = new FormControl(0);
  timeFrameForm = new FormGroup({
    start: new FormControl<Moment | null>(null, {updateOn: 'blur'}),
    end: new FormControl<Moment | null>(null, {updateOn: 'blur'})
  });

  statsWeek$: Observable<Map<string, Map<string, GraphStatsV2>>> // <date, stats>. Is array if there's line and bar chart stats within
    = this.statsV2$
    .pipe(
      filter(isNotUndefined),
      map((data: Map<string, Map<string, GraphStatsV2>>) => data)
    );

  constructor(public specimenGraphService: SpecimenGraphService) {
    this.timeFrameForm.valueChanges
      .pipe(startWith(null))
      .subscribe(range => {
        if (range) {
          if (moment(range.start, 'DD-MM-YYYY ', true).isValid()
              && moment(range.end, 'DD-MM-YYYY ', true).isValid()
              && this.timeFrameForm.valid) {
            let view = 'WEEK';
            if (this.viewForm.value === ViewV2.YEAR) view = 'YEAR';
            if (this.viewForm.value === ViewV2.EXPONENTIAL) view = 'EXPONENTIAL';

            this.specimenGraphService.getSpecimenDataCustom(view,
                moment(range.start).valueOf(),
                moment(range.end).valueOf())
              .pipe(filter(isNotUndefined))
              .subscribe(customData => {
                const mappedData: Map<string, Map<string, GraphStatsV2>> = new Map(Object.entries(customData.body));
                this.statsV2Subject.next(mappedData);
              });
          }
        }
      });

    this.statForm.valueChanges.pipe(filter(isNotNull))
      .subscribe(val => this.setStatValue(val));

    this.viewForm.valueChanges
      .pipe(
        filter(isNotNull),
        startWith(1)
      )
      .subscribe(view => { // 1 -> week, 2 -> month, 3 -> year, 4 -> combined
        this.clearCustomTimeFrame(false);
        if (view === ViewV2.WEEK) {
          this.specimenGraphService.specimenDataWeek$
            .pipe(filter(isNotUndefined))
            .subscribe(data => {
              const mappedData: Map<string, Map<string, GraphStatsV2>> = new Map(Object.entries(data.body));
              this.statsV2Subject.next(mappedData);
            });
        }
        if (view === ViewV2.MONTH) {
          this.specimenGraphService.specimenDataMonth$
            .pipe(filter(isNotUndefined))
            .subscribe(data => {
              const mappedData: Map<string, Map<string, GraphStatsV2>> = new Map(Object.entries(data.body));
              this.statsV2Subject.next(mappedData);
            });
        }
        if (view === ViewV2.YEAR || view === ViewV2.EXPONENTIAL) {
          this.specimenGraphService.specimenDataYear$
            .pipe(filter(isNotUndefined))
            .subscribe(data => {
              const mappedData: Map<string, Map<string, GraphStatsV2>> = new Map(Object.entries(data.body));
              if (view === ViewV2.YEAR) { // we don't need this if it's just year and not the mix
                mappedData.delete(ChartDataTypes.EXPONENTIAL);
              }
              this.statsV2Subject.next(mappedData);
            });
        }
      });
  }

  setStatValue(statValue: StatValue) {
    this.statValueSubject.next(statValue);
    this.statValue = statValue;
    if (statValue === StatValue.INSTITUTE) this.title = 'Specimens / Institution';
    if (statValue === StatValue.PIPELINE) this.title = 'Specimens / Pipeline';
    if (statValue === StatValue.WORKSTATION) this.title = 'Specimens / Workstation';
  }

  clearCustomTimeFrame(clearView: boolean) {
    this.timeFrameForm.reset();
    if (clearView) this.viewForm.setValue(this.viewForm.value, {emitEvent: true});
  }
}
