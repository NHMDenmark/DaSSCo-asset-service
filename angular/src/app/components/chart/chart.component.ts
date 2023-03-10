import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import Chart, {ChartDataset, ChartOptions} from 'chart.js/auto';
import {BehaviorSubject, combineLatest, filter, map} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {GraphData} from '../../types';

@Component({
  selector: 'dassco-line-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChartComponent {
  chart: any;
  readonly chartDataSubject = new BehaviorSubject<GraphData | undefined>(undefined);
  titleSubject = new BehaviorSubject<string>('');

  @Input()
  set setChartData(chartdata: GraphData) {
    this.chartDataSubject.next(chartdata);
  }

  @Input()
  set setTitle(title: string) {
    this.titleSubject.next(title);
  }

  setChartValues$
    = combineLatest([
    this.chartDataSubject.pipe(filter(isNotUndefined)),
    this.titleSubject
  ])
    .pipe(
      map(([chartData, title]) => {
        const lineDatasets: ChartDataset[] = this.createDataset(chartData);
        this.createchart(chartData.labels, lineDatasets, 'Specimens created', title);
      })
    );

  createDataset(graphData: GraphData): ChartDataset[] {
    const chartDatasets: ChartDataset[] = [];
    if (graphData.mainChart) { // key = institut, value = dato, amount
      graphData.mainChart.forEach((value: Map<string, number>, key: string) => {
        chartDatasets.push(this.addDataset(value, key, 'line', graphData, 0));
      });
    }
    if (graphData.subChart) {
      graphData.subChart.forEach((value: Map<string, number>, key: string) => {
        chartDatasets.push(this.addDataset(value, key, 'bar', graphData, null));
      });
    }
    return chartDatasets;
  }

  addDataset(value: Map<string, number>, key: string, type: string, graphData: GraphData, defaultVal: any): ChartDataset {
    const data: any[] = [];
    const pointRadius: number[] = [];
    graphData.labels.forEach((label, idx, labels) => {
      if (value.has(label)) {
        data.push(value.get(label)!);
        if (type === 'line') pointRadius.push(5);
      } else if (type === 'line') { // if it's pr year, we don't want the graph to go down to 0
        value.has(labels[idx - 1]) && graphData.timeFrame.period === 'YEAR' ? data.push(value.get(labels[idx - 1])!) : data.push(defaultVal);
        pointRadius.push(defaultVal);
      } else {
        data.push(defaultVal);
      }
    });
    return {
      type: type,
      label: graphData.multi ? key + ' <' + type + '>' : key,
      data: data,
      borderWidth: type === 'line' ? 2 : 1.5,
      pointRadius: pointRadius,
      barPercentage: type === 'line' ? null : 0.9,
      borderRadius: type === 'line' ? null : 5
    } as ChartDataset;
  }

  createchart(labels: string[], lineDataset: ChartDataset[], yaxis: string, title: string): void {
    if (this.chart) this.chart.destroy();
    this.chart = new Chart('line-chart', {
      data: {
        labels: labels,
        datasets: lineDataset
      },
      options: this.getOptions(yaxis, title)
    });
  }

  getOptions(yaxis: string, title: string): ChartOptions { // only way to specify the type so I don't get annoying errors ¯\_(ツ)_/¯
    return {
      responsive: true,
      maintainAspectRatio: true,
      aspectRatio: 2.5,
      skipNull: true,
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
          position: 'top',
          labels: {
            sort(a, b, _data) {
              return a.text.split(' <')[0].localeCompare(b.text.split(' <')[0]);
            }
          }
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
    } as ChartOptions;
  }
}
