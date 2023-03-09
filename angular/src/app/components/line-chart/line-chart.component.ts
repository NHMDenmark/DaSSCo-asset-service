import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import Chart, {ChartDataset, ChartType} from 'chart.js/auto';
import {BehaviorSubject, combineLatest, filter, map} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import {GraphData} from '../../types';
import {SpecimenGraphService} from "../../services/specimen-graph.service";

@Component({
  selector: 'dassco-line-chart',
  templateUrl: './line-chart.component.html',
  styleUrls: ['./line-chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LineChartComponent {
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
        const lineDatasets: ChartDataset[] = this.createDataset(chartData, 'line');
        this.createchart(chartData.labels, lineDatasets, 'Specimens created', title);
      })
    );

  constructor(private specimenGraphService: SpecimenGraphService) {
  }

  createDataset(graphData: GraphData, type: ChartType): ChartDataset[] {
    const chartDatasets: ChartDataset[] = [];
    if (graphData.mainChart) {
      graphData.mainChart.forEach((value: Map<string, number>, key: string) => {
        const data: number[] = [];
        const pointRadius: number[] = [];
        graphData.labels.forEach((label, idx, labels) => {
          if (value.has(label)) {
            data.push(value.get(label)!);
            pointRadius.push(5);
          } else { // if it's pr year, we don't want the graph to go down to 0
            value.has(labels[idx - 1]) && graphData.timeFrame.period === 'YEAR' ? data.push(value.get(labels[idx - 1])!) : data.push(0);
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
    }
    return chartDatasets;
  }

  createchart(labels: string[], lineDataset: ChartDataset[], yaxis: string, title: string): void {
    if (this.chart) this.chart.destroy();
    this.chart = new Chart('line-chart', {
      // type: 'line',
      data: {
        labels: labels,
        datasets: lineDataset
      },
      options: this.specimenGraphService.getGraphOptions(yaxis, title)
    });
  }
}
