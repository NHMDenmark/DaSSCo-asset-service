import {Component, EventEmitter, Input, Output} from '@angular/core';
import Chart, {ChartDataset} from 'chart.js/auto';
import randomColor from 'randomcolor';
import {BehaviorSubject, combineLatest, filter, map} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {defaultTimeFrame, TimeFrame} from '../../types';

@Component({
  selector: 'dassco-line-chart',
  templateUrl: './line-chart.component.html',
  styleUrls: ['./line-chart.component.scss']
})
export class LineChartComponent {
  chart: any;
  readonly chartDataSubject = new BehaviorSubject<Map<string, Map<string, number>> | undefined>(undefined);
  readonly labelsSubject = new BehaviorSubject<string[] | undefined>(undefined);
  timeFrameSubject = new BehaviorSubject<TimeFrame>(defaultTimeFrame);

  @Input()
  set setChartData(chartdata: Map<string, Map<string, number>>) {
    this.chartDataSubject.next(chartdata);
  }

  @Input()
  set setLabels(labels: string[]) {
    this.labelsSubject.next(labels);
  }

  @Output() timeFrame = new EventEmitter<TimeFrame>;

  setChartValues$
    = combineLatest([
    this.chartDataSubject.pipe(filter(isNotUndefined)),
    this.labelsSubject.pipe(filter(isNotUndefined)),
    this.timeFrameSubject
  ])
    .pipe(
      map(([chartData, labels, timeFrame]) => {
        this.setChart(chartData, labels, timeFrame);
      })
    );

  setChart(chartData: Map<string, Map<string, number>>, labels: string[], timeFrame: TimeFrame){
    const chartDatasets: ChartDataset[] = [];
    chartData.forEach((value: Map<string, number>, institute: string) => {
      const data: number[] = [];
      const pointRadius: number[] = [];
      labels.forEach((label, idx, labels) => {
        if (value.has(label)) {
          data.push(value.get(label)!);
          pointRadius.push(5);
        } else {
          value.has(labels[idx - 1]) && timeFrame.period === 'YEAR' ? data.push(value.get(labels[idx - 1])!) : data.push(0);
          pointRadius.push(0);
        }
      });
      chartDatasets.push({
        label: institute,
        data: data,
        borderWidth: 2,
        pointRadius: pointRadius,
        backgroundColor: randomColor({luminosity: 'light', format: 'hsl', seed: institute}),
        borderColor: randomColor({luminosity: 'light', format: 'hsl', seed: institute})
      });
    });

    this.createchart(labels, chartDatasets, 'Specimens created');
  }

  createchart(labels: string[], chartDatasets: ChartDataset[], yaxis: string): void {
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

  setTimeFrame(period: string, amount: number, unit: string, format: string) {
    this.timeFrameSubject.next({period: period, amount: amount, unit: unit, format: format} as TimeFrame);
    this.timeFrame.emit({period: period, amount: amount, unit: unit, format: format} as TimeFrame);
  }
}
