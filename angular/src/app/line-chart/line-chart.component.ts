import { Component, OnInit } from '@angular/core';
import Chart from 'chart.js/auto';
import {SpecimenGraphService} from "../services/specimen-graph.service";
import {filter, map} from "rxjs";
import { isNotUndefined } from '@northtech/ginnungagap';

@Component({
  selector: 'dassco-line-chart',
  templateUrl: './line-chart.component.html',
  styleUrls: ['./line-chart.component.scss']
})
export class LineChartComponent implements OnInit {
  public chart: any;

  graphInfo$
    = this.specimenGraphService.specimenGraphInfo$
    .pipe(
      filter(isNotUndefined),
      map(info => {
        console.log(info)

        return info;
      })
    )

  constructor(public specimenGraphService: SpecimenGraphService) { }

  ngOnInit(): void {
    this.createChart();
  }

  createChart(){
    this.chart = new Chart("lineChart", {
      type: 'line',

      data: {// values on X-Axis
        labels: ['2022-05-10', '2022-05-11', '2022-05-12','2022-05-13',
          '2022-05-14', '2022-05-15', '2022-05-16','2022-05-17', ],
        datasets: [
          {
            label: "Sales",
            data: ['467','576', '572', '79', '92',
              '574', '573', '576'],
            borderWidth: 1,
            backgroundColor: 'rgb(75, 192, 192)',
            borderColor: 'rgb(75, 192, 192)',
            pointRadius: 5
          },
          {
            label: "Profit",
            data: ['542', '542', '536', '327', '17',
              '0.00', '538', '541'],
            borderWidth: 1,
            backgroundColor: 'rgb(216, 130, 174)',
            borderColor: 'rgb(216, 130, 174)',
            pointRadius: 5
          }
        ]
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
}
