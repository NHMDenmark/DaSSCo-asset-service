import {AfterViewInit, Component, OnDestroy} from '@angular/core';
import {SpecimenGraphService} from '../../services/specimen-graph.service';
import {
  BehaviorSubject,
  distinctUntilChanged,
  filter,
  map,
  Observable,
  of,
  startWith, Subject,
  Subscription,
  switchMap, take, takeUntil,
} from 'rxjs';
import {
  defaultView,
  StatValue,
  GraphStatsV2, ViewV2, CUSTOM_DATE_FORMAT, ChartDataTypes
} from '../../types/graph-types';
import {isNotNull, isNotUndefined} from '@northtech/ginnungagap';
import moment, {Moment} from 'moment-timezone';
import {FormControl, FormGroup} from '@angular/forms';
import {DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE} from '@angular/material/core';
import {MAT_MOMENT_DATE_ADAPTER_OPTIONS, MomentDateAdapter} from '@angular/material-moment-adapter';
import {MatSnackBar} from "@angular/material/snack-bar";
import {HttpStatusCode} from "@angular/common/http";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from "@angular/common";


@Component({
  selector: 'dassco-graph-data',
  templateUrl: './graph-data.component.html',
  styleUrls: ['./graph-data.component.scss'],
  providers: [
    { provide: DateAdapter, useClass: MomentDateAdapter, deps: [MAT_DATE_LOCALE,
        MAT_MOMENT_DATE_ADAPTER_OPTIONS] },
    { provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS, useValue: { useUtc: true } },
    {
      provide: MAT_DATE_FORMATS, useValue: CUSTOM_DATE_FORMAT
    }
  ]
})
export class GraphDataComponent implements AfterViewInit, OnDestroy {
  chart: any;
  viewForm = new FormControl(defaultView);
  statValueSubject = new BehaviorSubject<StatValue>(StatValue.INSTITUTE);
  statsV2Subject = new BehaviorSubject<Map<string, Map<string, GraphStatsV2>> | undefined>(undefined); // incremental: {dato, data}, exponential: {dato, data}
  statsV2$ = this.statsV2Subject.asObservable();
  title = 'Specimens / Institute';
  private destroy = new Subject<boolean>()
  statValue = StatValue.INSTITUTE; // the statistics chosen -> institute, pipeline, workstation
  statForm = new FormControl(0);
  timeFrameForm = new FormGroup({
    start: new FormControl<Moment | null>(null, {updateOn: 'blur'}),
    end: new FormControl<Moment | null>(null, {updateOn: 'blur'})
  });
  currentViewSubscription: Subscription | undefined; // we subscribe to the weekly/monthly/yearly data observable, and need to unsub when we change to avoid multiple subs at a time

  statsWeek$: Observable<Map<string, Map<string, GraphStatsV2>>> // <date, stats>. Is array if there's line and bar chart stats within
    = this.statsV2$
    .pipe(
      filter(isNotUndefined),
      map((data: Map<string, Map<string, GraphStatsV2>>) => {
        return data;
      })
    );

  startDate: string | null = null;
  endDate: string | null = null;

  constructor(public specimenGraphService: SpecimenGraphService
    , private snackBar: MatSnackBar, private route : ActivatedRoute,
              private router : Router, private location : Location) {


     this.timeFrameForm.valueChanges.pipe(startWith(null), distinctUntilChanged(), filter((range) => !!range?.start && !!range?.end), switchMap((range) => {
        if (range) {
          if (moment(range.start, 'DD-MM-YYYY ', true).isValid()
            && moment(range.end, 'DD-MM-YYYY ', true).isValid()
            && this.timeFrameForm.valid) {
            let view = "WEEK";
            if (this.viewForm.value === ViewV2.YEAR) view = 'YEAR';
            if (this.viewForm.value === ViewV2.EXPONENTIAL) view = 'EXPONENTIAL';
            // this is stupid but otherwise it fucks up both zone and time and it's all wrong. time is limited. sorry
            const endDateFormatted = moment(moment(range.end).format('YYYY-MM-DDTHH:mm:ss')).endOf('day');

            this.router.navigate([], {
              queryParamsHandling: 'merge',
              queryParams: {
                startDate: range.start!.format('DD-MM-YYYY'),
                endDate: range.end!.format('DD-MM-YYYY'),
                type: 'custom'
              }
            })
            return this.specimenGraphService.getSpecimenDataCustom(view,
              moment(range.start).valueOf(),
              endDateFormatted.valueOf())
              .pipe(filter(isNotUndefined))
          }
          return of(null)
        } else {
          return of(null);
        }
      }), takeUntil(this.destroy))
      .subscribe(customData => {
        if (!customData) {
          this.statsV2Subject.next(undefined)
        }
        if (!customData?.ok) {
          this.statsV2Subject.next(undefined)
        }
        const mappedData: Map<string, Map<string, GraphStatsV2>> = new Map(Object.entries(customData!.body));
        this.statsV2Subject.next(mappedData);
      });

      this.statForm.valueChanges.pipe(
        filter(isNotNull),
        distinctUntilChanged(), takeUntil(this.destroy))
        .subscribe(val => {
          let queryParams = { ... this.route.snapshot.queryParams };
          if (val == 0){
            queryParams['statValue'] = 'institution';
            const newQueryString = Object.keys(queryParams)
              .map(key => `${key}=${queryParams[key]}`)
              .join("&");
            this.location.replaceState(this.router.url.split('?')[0], newQueryString);
          } else if (val == 1){
            queryParams['statValue'] = 'pipeline';
            const newQueryString = Object.keys(queryParams)
              .map(key => `${key}=${queryParams[key]}`)
              .join("&");
            this.location.replaceState(this.router.url.split('?')[0], newQueryString);
          } else if (val == 2){
            queryParams['statValue'] = 'workstation';
            const newQueryString = Object.keys(queryParams)
              .map(key => `${key}=${queryParams[key]}`)
              .join("&");
            this.location.replaceState(this.router.url.split('?')[0], newQueryString);
          }
          this.setStatValue(val)
        });

    this.viewForm.valueChanges
      .pipe(
        filter(isNotNull),
        startWith(this.viewForm.value),
        distinctUntilChanged(),
        takeUntil(this.destroy)
      )
      .subscribe(view => { // 1 -> week, 2 -> month, 3 -> year, 4 -> combined

        this.clearCustomTimeFrame(false);
        this.currentViewSubscription?.unsubscribe();

        let queryParams = { ... this.route.snapshot.queryParams };

        if (view === ViewV2.WEEK) {
          queryParams['type'] = 'week';
          const newQueryString = Object.keys(queryParams)
            .map(key => `${key}=${queryParams[key]}`)
            .join("&");
          this.location.replaceState(this.router.url.split('?')[0], newQueryString);
          this.currentViewSubscription = this.specimenGraphService.specimenDataWeek$
            .pipe(
              filter(isNotUndefined),
              distinctUntilChanged((prev, curr) => JSON.stringify(prev.body) === JSON.stringify(curr.body))
            )
            .subscribe(data => {
              const mappedData: Map<string, Map<string, GraphStatsV2>> = new Map(Object.entries(data.body));
              this.statsV2Subject.next(mappedData);
            });
        }
        if (view === ViewV2.MONTH) {
          queryParams['type'] = 'month';
          const newQueryString = Object.keys(queryParams)
            .map(key => `${key}=${queryParams[key]}`)
            .join("&");
          this.location.replaceState(this.router.url.split('?')[0], newQueryString);

          this.currentViewSubscription = this.specimenGraphService.specimenDataMonth$
            .pipe(
              filter(isNotUndefined),
              distinctUntilChanged((prev, curr) => JSON.stringify(prev.body) === JSON.stringify(curr.body))
            )
            .subscribe(data => {
              const mappedData: Map<string, Map<string, GraphStatsV2>> = new Map(Object.entries(data.body));
              this.statsV2Subject.next(mappedData);
            });
        }
        if (view === ViewV2.YEAR || view === ViewV2.EXPONENTIAL) {
          if (view === ViewV2.YEAR){
            queryParams['type'] = 'year';
            const newQueryString = Object.keys(queryParams)
              .map(key => `${key}=${queryParams[key]}`)
              .join("&");
            this.location.replaceState(this.router.url.split('?')[0], newQueryString);
          } else {
            queryParams['type'] = 'exponential';
            const newQueryString = Object.keys(queryParams)
              .map(key => `${key}=${queryParams[key]}`)
              .join("&");
            this.location.replaceState(this.router.url.split('?')[0], newQueryString);
          }
          this.currentViewSubscription = this.specimenGraphService.specimenDataYear$
            .pipe(
              filter(isNotUndefined),
              distinctUntilChanged((prev, curr) => JSON.stringify(prev.body) === JSON.stringify(curr.body))
            )
            .subscribe(data => {
              const mappedData: Map<string, Map<string, GraphStatsV2>> = new Map(Object.entries(data.body));
              if (view === ViewV2.YEAR) { // we don't need this if it's just year and not the mix
                mappedData.delete(ChartDataTypes.EXPONENTIAL);
                console.log('mapped', mappedData)
              }
              this.statsV2Subject.next(mappedData);
            });
        }
        if (view === ViewV2.CUSTOM){
          queryParams['type'] = 'custom';
          const newQueryString = Object.keys(queryParams)
            .map(key => `${key}=${queryParams[key]}`)
            .join("&");
          this.location.replaceState(this.router.url.split('?')[0], newQueryString);
          this.timeFrameForm.patchValue({
            start: moment(this.startDate, 'DD-MM-YYYY'),
            end: moment(this.endDate, 'DD-MM-YYYY')
          })
        }
      });
  }

  ngOnDestroy(): void {
        this.destroy.next(true)
    this.currentViewSubscription?.unsubscribe()
    }

  ngAfterViewInit(): void {
    this.route.queryParamMap.pipe(take(1)).subscribe(params => {
      // type can be week/month/total/total+fluctuation
      const type = params.get("type");

      if (type?.toLowerCase() == "week" || type == null) {
        this.viewForm.setValue(ViewV2.WEEK);
      } else if (type?.toLowerCase() == "month") {
        this.viewForm.setValue(ViewV2.MONTH);
      } else if (type?.toLowerCase() == "year") {
        this.viewForm.setValue(ViewV2.YEAR);
      } else if (type?.toLowerCase() == "exponential") {
        this.viewForm.setValue(ViewV2.EXPONENTIAL);
      } else if (type?.toLowerCase() == "custom"){
        this.startDate = params.get("startDate");
        this.endDate = params.get("endDate");
        if (moment(this.startDate, 'DD-MM-YYYY').isValid() && moment(this.endDate, 'DD-MM-YYYY').isValid()){
          this.viewForm.setValue(ViewV2.CUSTOM);
        }
      }

      const statValue = params.get("statValue")
      if (statValue?.toLowerCase() == "institution" || statValue == null){
        this.statForm.setValue(0);
      } else if (statValue?.toLowerCase() == "pipeline"){
        this.statForm.setValue(1);
      } else if (statValue?.toLowerCase() == "workstation"){
        this.statForm.setValue(2);
      }
    })
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

  refreshGraph() {
    this.specimenGraphService.refreshGraph()
      .pipe(
        filter(isNotUndefined)
      )
      .subscribe(response => {
        if (response.ok) {
          this.viewForm.updateValueAndValidity({onlySelf: false, emitEvent: true});
          this.openSnackBar("Cache has been refreshed", "OK")
        } else if (response.status == HttpStatusCode.NoContent) {
          this.openSnackBar("No data in cache to be refreshed", "OK")
        } else {
          this.openSnackBar("An error occurred and cache was not refreshed", "OK")
        }
      })
  }

  refreshPage(){
    this.location.go(this.location.path());
    window.location.reload();
  }

  openSnackBar(message: string, action: string) {
    this.snackBar.open(message, action, {duration: 3000});
  }
}

