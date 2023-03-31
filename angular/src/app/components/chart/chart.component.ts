import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import Chart, {
  ChartDataset, ChartEvent,
  ChartOptions, LegendElement, LegendItem
} from 'chart.js/auto';
import {BehaviorSubject, combineLatest, filter, map} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {GraphData} from '../../types';
import zoomPlugin from 'chartjs-plugin-zoom';

@Component({
  selector: 'dassco-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class ChartComponent {
  chart: any;
  readonly chartDataSubject = new BehaviorSubject<GraphData | undefined>(undefined);
  titleSubject = new BehaviorSubject<string>('');
  clickedLabels: string[] = [];

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
        if (chartData.mainChart && chartData.mainChart.size <= 0 && !chartData.subChart) { // mainchart is set immediately as empty/free Map in graph-data, thus it isn't undefined
          this.createchart([], [], '', 'No data available for the selected dates');
        } else {
          const lineDatasets: ChartDataset[] = this.createDataset(chartData);
          this.createchart(chartData.labels, lineDatasets, 'Specimens created', title);
        }
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
      label: key,
      data: data,
      borderWidth: type === 'line' ? 2 : 1.5,
      pointRadius: pointRadius,
      borderRadius: type === 'line' ? null : 5,
      order: type === 'line' ? 2 : 1,
      stack: type,
      hidden: this.clickedLabels.includes(key)
    } as ChartDataset;
  }

  createchart(labels: string[], dataset: ChartDataset[], yaxis: string, title: string): void {
    if (this.chart) this.chart.destroy();
    Chart.register(zoomPlugin);
    // if (dataset.length <= 0) {
    //   yaxis = '';
    //   title = 'No data available for the selected dates';
    // }

    this.chart = new Chart('canvas', {
      data: {
        labels: labels,
        datasets: dataset
      },
      options: this.getOptions(yaxis, title)
    });
  }

  getOptions(yaxis: string, title: string): ChartOptions { // only way to specify the type so I don't get annoying errors ¯\_(ツ)_/¯
    return {
      responsive: true,
      maintainAspectRatio: true,
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
          onClick: this.clickHandler
        },
        zoom: {
          zoom: {
            wheel: {
              enabled: true
            },
            pinch: {
              enabled: true
            },
            mode: 'xy'
          }
        }
      },
      scales: {
        y: {
          stacked: true,
          ticks: {
            beginAtZero: true,
            callback(val, _index) {
              return val as number % 1 === 0 ? val : '';
            }
          },
          title: {
            display: true,
              align: 'center',
              text: yaxis
          }
        }
      }
    } as ChartOptions;
  }

  clickHandler = (_e: ChartEvent, legendItem: LegendItem, legend: LegendElement<any>) => {
    if (this.clickedLabels.includes(legendItem.text)) {
      const idx = this.clickedLabels.indexOf(legendItem.text, 0);
      this.clickedLabels.splice(idx);
    } else {
      this.clickedLabels.push(legendItem.text);
    }
    this.defaultLabelClick(legendItem, legend);
  };

  defaultLabelClick(legendItem: LegendItem, legend: LegendElement<any>) {
    const index = legendItem.datasetIndex;
    const ci = legend.chart;
    if (ci.isDatasetVisible(index!)) {
      ci.hide(index!);
      legendItem.hidden = true;
    } else {
      ci.show(index!);
      legendItem.hidden = false;
    }
  }
}
