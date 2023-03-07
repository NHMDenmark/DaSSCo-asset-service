import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import Chart, {ChartDataset} from 'chart.js/auto';
import {BehaviorSubject, combineLatest, filter, map} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {defaultTimeFrame, TimeFrame} from '../../types';

@Component({
  selector: 'dassco-line-chart',
  templateUrl: './line-chart.component.html',
  styleUrls: ['./line-chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LineChartComponent {
  chart: any;
  readonly chartDataSubject = new BehaviorSubject<Map<string, Map<string, number>> | undefined>(undefined);
  readonly labelsSubject = new BehaviorSubject<string[] | undefined>(undefined);
  timeFrameSubject = new BehaviorSubject<TimeFrame>(defaultTimeFrame);
  titleSubject = new BehaviorSubject<string>('');
  // timeFrameForm = new FormControl(1);
  // timeFrameMap: Map<number, {period: string, amount: number, unit: string, format: string}> = new Map([
  //   [1, {period: 'WEEK', amount: 7, unit: 'days', format: 'DD-MMM-YY'}],
  //   [2, {period: 'MONTH', amount: 30, unit: 'days', format: 'DD-MMM-YY'}],
  //   [3, {period: 'YEAR', amount: 12, unit: 'months', format: 'MMM YY'}]
  // ]);

  @Input()
  set setChartData(chartdata: Map<string, Map<string, number>>) {
    this.chartDataSubject.next(chartdata);
  }

  @Input()
  set setTitle(title: string) {
    this.titleSubject.next(title);
  }

  @Input()
  set setLabels(labels: string[]) {
    this.labelsSubject.next(labels);
  }

  @Input()
  set setTimeFrame(timeFrame: TimeFrame) {
    this.timeFrameSubject.next(timeFrame);
  }

  @Output() timeFrame = new EventEmitter<TimeFrame>;

  setChartValues$
    = combineLatest([
    this.chartDataSubject.pipe(filter(isNotUndefined)),
    this.labelsSubject.pipe(filter(isNotUndefined)),
    this.timeFrameSubject,
    this.titleSubject
  ])
    .pipe(
      map(([chartData, labels, timeFrame, title]) => {
        this.setChart(chartData, labels, timeFrame, title);
      })
    );

  // constructor() {
    // this.timeFrameForm.valueChanges.pipe(filter(isNotNull))
    //   .subscribe(val => {
    //   this.timeFrameSubject.next(this.timeFrameMap.get(val) as TimeFrame);
    //   this.timeFrame.emit(this.timeFrameMap.get(val) as TimeFrame);
    // });
  // }

  setChart(chartData: Map<string, Map<string, number>>, labels: string[], timeFrame: TimeFrame, title: string) {
    const chartDatasets: ChartDataset[] = [];
    chartData.forEach((value: Map<string, number>, key: string) => {
      const data: number[] = [];
      const pointRadius: number[] = [];
      // const color = chroma(chroma.random()).luminance(0.5).saturate(1.5).hex();
      // console.log(color)
      labels.forEach((label, idx, labels) => {
        if (value.has(label)) {
          data.push(value.get(label)!);
          pointRadius.push(5);
        } else { // if it's pr year, we don't want the graph to go down to 0
          value.has(labels[idx - 1]) && timeFrame.period === 'YEAR' ? data.push(value.get(labels[idx - 1])!) : data.push(0);
          pointRadius.push(0);
        }
      });
      chartDatasets.push({
        label: key,
        data: data,
        borderWidth: 2,
        pointRadius: pointRadius
        // backgroundColor: color,
        // borderColor: color
      });
    });

    this.createchart(labels, chartDatasets, 'Specimens created', title);
  }

  createchart(labels: string[], chartDatasets: ChartDataset[], yaxis: string, title: string): void {
    if (this.chart) this.chart.destroy();
    this.chart = new Chart('line-chart', {
      type: 'line',
      data: {
        labels: labels,
        datasets: chartDatasets
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        aspectRatio: 2.5,
        layout: {
          padding: 10
        },
        plugins: {
          title: {
            display: true,
            text: title,
            font: {
              size: 20
            }
          },
          legend: {
            position: 'top'
          }
        },
        scales: {
          y: {
            title: {
              display: true,
              align: 'center',
              text: yaxis
            },
            ticks: {
              callback(val, _index) {
                return val as number % 1 === 0 ? val : '';
              }
            }
          }
        }
      }
    });
  }
}
