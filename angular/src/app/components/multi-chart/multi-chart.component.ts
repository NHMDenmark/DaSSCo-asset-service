import {Component, Input} from '@angular/core';
import {BehaviorSubject, combineLatest, filter, map} from 'rxjs';
import {GraphData} from '../../types';
import {SpecimenGraphService} from '../../services/specimen-graph.service';
import {isNotUndefined} from '@northtech/ginnungagap';
import Chart, {ChartDataset} from 'chart.js/auto';
import randomColor from 'randomcolor';

@Component({
  selector: 'dassco-multi-chart',
  templateUrl: './multi-chart.component.html',
  styleUrls: ['./multi-chart.component.scss']
})
export class MultiChartComponent {
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

  constructor(private specimenGraphService: SpecimenGraphService) { }

  createDataset(graphData: GraphData): ChartDataset[] {
    const chartDatasets: ChartDataset[] = [];
    if (graphData.mainChart && graphData.subChart) { // key = institut, value = dato, amount
      graphData.mainChart.forEach((value: Map<string, number>, key: string) => {
        chartDatasets.push(this.addDataset(value, key, 'line', graphData, 0));
      });
      graphData.subChart.forEach((value: Map<string, number>, key: string) => {
        chartDatasets.push(this.addDataset(value, key, 'bar', graphData, null));
        // const data: any[] = [];
        // graphData.labels.forEach((label) => {
        //   if (value.has(label)) {
        //     data.push(value.get(label)!);
        //   } else { // if it's pr year, we don't want the graph to go down to 0
        //     data.push(null);
        //   }
        // });
        // chartDatasets.push({
        //   type: 'bar',
        //   label: key + '_bar',
        //   data: data,
        //   backgroundColor: randomColor({luminosity: 'light', format: 'rgba', alpha: 0.6, seed: key}),
        //   borderColor: randomColor({luminosity: 'light', format: 'rgb', seed: key}),
        //   borderWidth: 1.5,
        //   borderRadius: 5,
        //   barPercentage: 1
        // });
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
    const color = type === 'line' ? randomColor({luminosity: 'light', format: 'rgb', seed: key}) : randomColor({luminosity: 'light', format: 'rgba', alpha: 0.6, seed: key});
    return {
      type: type,
      label: key + '_' + type,
      pointBackgroundColor: color,
      borderColor: type === 'line' ? color : randomColor({luminosity: 'light', format: 'rgb', seed: key}), // we don't want alpha if it's a bar for the border
      backgroundColor: color,
      data: data,
      borderWidth: type === 'line' ? 2 : 1.5,
      pointRadius: pointRadius,
      barPercentage: type === 'line' ? null : 0.9,
      borderRadius: type === 'line' ? null : 5
    } as ChartDataset;
  }

  createchart(labels: string[], lineDataset: ChartDataset[], yaxis: string, title: string): void {
    if (this.chart) this.chart.destroy();
    this.chart = new Chart('multi-chart', {
      data: {
        labels: labels.sort((a, b) => a.localeCompare(b)),
        datasets: lineDataset
      },
      options: this.specimenGraphService.getGraphOptions(yaxis, title)
    });
  }
}
