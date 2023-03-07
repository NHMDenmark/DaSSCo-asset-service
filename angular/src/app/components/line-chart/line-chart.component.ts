import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import Chart, {ChartDataset, ChartType} from 'chart.js/auto';
import {BehaviorSubject, combineLatest, filter, map} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {defaultTimeFrame, GraphData, TimeFrame} from '../../types';

@Component({
  selector: 'dassco-line-chart',
  templateUrl: './line-chart.component.html',
  styleUrls: ['./line-chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LineChartComponent {
  chart: any;
  readonly chartDataSubject = new BehaviorSubject<GraphData | undefined>(undefined);
  readonly labelsSubject = new BehaviorSubject<string[] | undefined>(undefined);
  timeFrameSubject = new BehaviorSubject<TimeFrame>(defaultTimeFrame);
  titleSubject = new BehaviorSubject<string>('');

  @Input()
  set setChartData(chartdata: GraphData) {
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

  setChart(chartData: GraphData, labels: string[], timeFrame: TimeFrame, title: string) {
    const lineDatasets: ChartDataset[] = this.createDataset(chartData.lineChart, labels, timeFrame, 'line');

    if (chartData.barChart) {
      const test = this.createDataset(chartData.barChart, labels, timeFrame, 'bar');
      lineDatasets.concat(test);
    }
    console.log(chartData);
    this.createchart(labels, lineDatasets, 'Specimens created', title);
  }

  createDataset(data: Map<string, Map<string, number>>, labels: string[], timeFrame: TimeFrame, type: ChartType): ChartDataset[] {
    const chartDatasets: ChartDataset[] = [];
    data.forEach((value: Map<string, number>, key: string) => {
      const data: number[] = [];
      const pointRadius: number[] = [];
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
        type: type,
        label: key,
        data: data,
        borderWidth: 2,
        pointRadius: pointRadius
      });
    });
    return chartDatasets;
  }

  createchart(labels: string[], lineDataset: ChartDataset[], yaxis: string, title: string): void {
    if (this.chart) this.chart.destroy();
    this.chart = new Chart('line-chart', {
      type: 'line',
      data: {
        labels: labels,
        datasets: lineDataset
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
            },
            color: 'rgba(20, 48, 82, 0.9)'
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
