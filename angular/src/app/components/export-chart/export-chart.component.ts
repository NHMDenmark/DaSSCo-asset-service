import {Component, Input} from '@angular/core';
import {BehaviorSubject, filter, take} from 'rxjs';
import {Chart} from 'chart.js';
import excelJS from 'exceljs';
import {saveAs} from 'file-saver';
import {combineLatest} from 'rxjs';
import {isNotUndefined} from '@northtech/ginnungagap';
import JsPDF from 'jspdf';
import {MatSnackBar} from "@angular/material/snack-bar";

@Component({
  selector: 'dassco-export-chart',
  templateUrl: './export-chart.component.html',
  styleUrls: ['./export-chart.component.scss']
})
export class ExportChartComponent {
  chartSubject = new BehaviorSubject<Chart | undefined>(undefined);
  titleSubject = new BehaviorSubject<string>('exported-graph');

  @Input()
  set setChart(chart: Chart) {
    this.chartSubject.next(chart);
  }

  @Input()
  set setTitle(title: string) {
    this.titleSubject.next(title);
  }

  constructor(private snackBar: MatSnackBar) {
  }

  downloadPdf() {
    combineLatest([
      this.chartSubject.asObservable().pipe(filter(isNotUndefined), take(1)),
      this.titleSubject.asObservable()
    ])
      .subscribe(([chart, title]) => { // i know... subscribe... i'm sorry.
        const chartElement: HTMLCanvasElement | null = document.querySelector('#canvas');
        if (chartElement) {
          chartElement.toBlob((blob) => {
            if (blob) {
              const pdf = new JsPDF('l', 'pt', [chartElement.width, chartElement.height]);
              pdf.addImage(chart.toBase64Image(), 0, 0, chartElement.width, chartElement.height);
              pdf.save(title + '.pdf');
              this.openSnackBar('File has been successfully downloaded', 'OK');
            }
          });
        } else {
          this.openSnackBar('An error has occurred. Try again.', 'OK');
        }
      });
  }

  downloadCsv() {
    combineLatest([
      this.chartSubject.asObservable().pipe(filter(isNotUndefined), take(1)),
      this.titleSubject.asObservable()
    ])
      .subscribe(([chart, title]) => { // i know... subscribe... i'm sorry.
        const workbook = new excelJS.Workbook;
        const worksheet = workbook.addWorksheet('Graph data');
        worksheet.columns = [
          {header: 'Label', key: 'label'},
          {header: 'Date', key: 'date'},
          {header: 'Amount', key: 'amount'}
        ];
        if (chart.data.datasets.length > 0) {
          const labels = chart.data.labels;
          chart.data.datasets.forEach((dataset) => {
            dataset.data.forEach((data, idx) => {
              worksheet.addRow({label: dataset.label, date: labels ? labels[idx] : 'N/A', amount: data ? data : '0'});
            });
          });
          workbook.xlsx.writeBuffer().then((buffer) => {
            const blob = new Blob([buffer], {type: 'application/xlsx'});
            saveAs(blob, title + '.xlsx');
            this.openSnackBar('File has been successfully downloaded', 'OK');
          });
        } else {
          this.openSnackBar('Seems there\'s no data to download. Try a different range.', 'OK');
        }
      });
  }

  openSnackBar(message: string, action: string) {
    this.snackBar.open(message, action, {duration: 3000});
  }
}
