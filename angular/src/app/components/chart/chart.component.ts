import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import Chart, {ChartDataset, ChartEvent, ChartOptions, LegendElement, LegendItem} from 'chart.js/auto';
import {BehaviorSubject, combineLatest, filter, map} from 'rxjs';
import {ChartDataTypes, GraphStatsV2, StatValue} from '../../types';
import zoomPlugin from 'chartjs-plugin-zoom';
import {isNotUndefined} from '@northtech/ginnungagap';

@Component({
  selector: 'dassco-chart',
  templateUrl: './chart.component.html',
  styleUrls: ['./chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class ChartComponent {
  chart: any;
  readonly chartDataSubjectV2 = new BehaviorSubject<Map<string, Map<string, GraphStatsV2>> | undefined>(undefined);
  titleSubject = new BehaviorSubject<string>('');
  clickedLabels: string[] = [];
  statValueSubject = new BehaviorSubject<StatValue>(StatValue.INSTITUTE);

  @Input()
  set setChartDataV2(chartdata: Map<string, Map<string, GraphStatsV2>>) {
    this.chartDataSubjectV2.next(chartdata);
  }

  @Input()
  set setStatValue(statValue: StatValue) {
    this.statValueSubject.next(statValue);
  }

  @Input()
  set setTitle(title: string) {
    this.titleSubject.next(title);
  }

  setChartValuesV2$
    = combineLatest([
    this.chartDataSubjectV2.pipe(filter(isNotUndefined)),
    this.titleSubject,
    this.statValueSubject
  ])
    .pipe(
      map(([chartData, title, statValue] : [Map<string, Map<string, GraphStatsV2>>, string, StatValue]) => {
        const incrMap = new Map(Object.entries(chartData.get(ChartDataTypes.INCREMENTAL) as Map<string, GraphStatsV2>));
        let incrDatasets = this.createDatasetV2(incrMap, statValue, 'line');
        const labels = Array.from(incrMap.keys()); // datoer, x-akse

        if (chartData.has(ChartDataTypes.EXPONENTIAL)) {
          const exponMap = new Map(Object.entries(chartData.get(ChartDataTypes.EXPONENTIAL) as Map<string, GraphStatsV2>));
          const exponDatasets = this.createDatasetV2(exponMap, statValue, 'bar');
          incrDatasets = incrDatasets.concat(exponDatasets);
        }

        this.createChart(labels, incrDatasets, 'Specimens Created', title);
      })
    );

  // For reference: "stat" referes to either institution, pipeline, or workstation
  createDatasetV2(chartData: Map<string, GraphStatsV2>, statValue: StatValue, type: string): ChartDataset[] {
    const chartDatasets: ChartDataset[] = [];
    const statName: Set<string> = new Set<string>(); // don't want duplicates
    let tempStatName: Array<string> = [];

    // getting all institution/pipe/work names from the data
    chartData.forEach((stats: GraphStatsV2, _date: string) => {
      const selectedStatMap: Map<string, number> = new Map(Object.entries(this.getKey(stats, statValue)));
      tempStatName = tempStatName.concat(Array.from(selectedStatMap.keys()));
    });
    tempStatName.forEach(i => statName.add(i));

    for (const name of statName) { // e.g. NNAD
      const data: any[] = [];
      const pointRadius: number[] = [];

      chartData.forEach((stats: GraphStatsV2, _date: string) => {
        const selectedStatMap: Map<string, number> = new Map(Object.entries(this.getKey(stats, statValue)));
        if (selectedStatMap.has(name)) {
          data.push(selectedStatMap.get(name) as number);
          pointRadius.push(5);
        } else {
          data.push(0);
          pointRadius.push(1);
        }
      });

      const tempDataset = {
        type: type,
        label: name,
        data: data,
        borderWidth: type === 'line' ? 2 : 1.5,
        pointRadius: pointRadius,
        borderRadius: type === 'line' ? null : 5,
        order: type === 'line' ? 2 : 1,
        stack: type,
        spanGaps: true,
        hidden: this.clickedLabels.includes(name)
      } as ChartDataset;

      chartDatasets.push(tempDataset);
    }
    return chartDatasets;
  }

  getKey(stats: GraphStatsV2, statValue: StatValue): Map<string, number> {
    // as I want to reuse the datasetcreation code, but don't know if we're looking at institute, pipeline, or workstation
    if (statValue === StatValue.INSTITUTE) return stats.institutes;
    if (statValue === StatValue.PIPELINE) return stats.pipelines;
    return stats.workstations;
  }

  createChart(labels: string[], dataset: ChartDataset[], yaxis: string, title: string): void {
    if (this.chart) this.chart.destroy();
    Chart.register(zoomPlugin);

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
          beginAtZero: true,
          stacked: false,
          ticks: {
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
