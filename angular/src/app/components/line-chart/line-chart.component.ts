import {Component, OnInit} from '@angular/core';
import Chart, {ChartDataset} from 'chart.js/auto';
import {SpecimenGraphService} from '../../services/specimen-graph.service';
import {BehaviorSubject, combineLatest, filter, map, Observable} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {InstituteService} from "../../services/institute.service";
import moment from "moment";
import {SpecimenGraph, TimeFrame} from "../../types";
import randomColor from 'randomcolor';

@Component({
  selector: 'dassco-line-chart',
  templateUrl: './line-chart.component.html',
  styleUrls: ['./line-chart.component.scss']
})
export class LineChartComponent implements OnInit {
  public chart: any;
  chartDataSubject = new BehaviorSubject<Map<string, Map<string, number>> | undefined>(undefined)
  timeFrameSubject = new BehaviorSubject<TimeFrame>(TimeFrame.MONTH);
  // chartLabels: string[] = [];

  institutes$
    = this.instituteService.institutes$
    .pipe(
      filter(isNotUndefined),
      map(institutes => {
        console.log(institutes)
        // this.chartLabels = institutes;
        // institutes.forEach(i => {
        //   console.log(date);
        // })
        return institutes;
      })
    )

  graphInfo$: Observable<SpecimenGraph[]>
    = combineLatest([
    this.specimenGraphService.specimenGraphInfo$.pipe(filter(isNotUndefined)),
    this.timeFrameSubject
  ])
    .pipe(
      map(([specimens, timeFrame]) => {
        const now = moment();
        const map = new Map<string, Map<string, number>>();
        specimens.forEach(s => {
          if (now.diff(moment(s.createdDate), 'days') <= timeFrame) { // if it's within the timeframe
            const created = moment(s.createdDate).format('DD-MMM-YY'); // unix timestamp
            if (map.has(s.instituteName)) { // if it exists
              const inst = map.get(s.instituteName);
              if (inst) {
                if (inst.has(created)) { // if its value has been set, we up the total
                  let total = inst.get(created);
                  if (total) inst.set(created, total + 1); // splitting them up to satisfy typescripts need for undefined-validation sigh
                } else { // otherwise, we set the total to 1
                  inst.set(created, 1);
                }
              }
            } else { // if nothing, we create a new with the institute name
              map.set(s.instituteName, new Map<string, number>([[created, 1]]))
            }
          }
        })
        this.chartDataSubject.next(map);
        return specimens;
      })
    )

  setChartValues$
    = combineLatest([
    this.chartDataSubject.pipe(filter(isNotUndefined)),
    this.timeFrameSubject
  ])
    .pipe(
      map(([chartData, timeFrame]) => {
        const labels = this.createLabels(timeFrame);
        this.createChart(chartData, labels);
      })
    )

  constructor(
    public specimenGraphService: SpecimenGraphService
    , public instituteService: InstituteService
  ) { }

  ngOnInit(): void {
  }

  createChart(chartData: Map<string, Map<string, number>>, labels: string[]){
    const chartDatasets: ChartDataset[] = [];
    chartData.forEach((value: Map<string, number>, institute: string) => {
      const data: number[] = [];
      const pointRadius: number[] = [];
      const color = randomColor({luminosity: 'light', format: 'hex'});
      labels.forEach(label => {
        if (value.has(label)) {
          data.push(value.get(label)!);
          pointRadius.push(5);
        } else {
          data.push(0);
          pointRadius.push(0);
        }
      })
      chartDatasets.push({
        label: institute,
        data: data,
        borderWidth: 1,
        pointRadius: pointRadius,
        backgroundColor: color,
        borderColor: color
      })
    });

    if (this.chart) this.chart.destroy();
    this.chart = new Chart("lineChart", {
      type: 'line',
      data: {
        labels: labels,
        datasets: chartDatasets
      },
      options: {
        aspectRatio:2.5,
        plugins: {
          legend: {
            position: 'left'
          }
        }
      }
    });
  }

  createLabels(timeFrame: TimeFrame): string[] {
    const labels: string[] = [];
    if (timeFrame === TimeFrame.WEEK || timeFrame === TimeFrame.MONTH) {
      for (let i = timeFrame - 1; i >= 0; i--) {
        labels.push(moment().subtract(i, 'days').format('DD-MMM-YY'));
      }
    } else { // year
      // tbc
      // console.log(moment().week())
      // console.log(moment().weeks())
    }
    return labels;
  }

  setTimeFrame(timeFrame: TimeFrame) {
    this.timeFrameSubject.next(timeFrame);
  }
}
