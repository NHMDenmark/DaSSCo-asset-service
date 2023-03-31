import {Component, Input} from '@angular/core';
import {Chart} from 'chart.js';
import excelJS from 'exceljs';
import {saveAs} from 'file-saver';
import JsPDF from 'jspdf';
import {MatSnackBar} from "@angular/material/snack-bar";

@Component({
  selector: 'dassco-export-chart',
  templateUrl: './export-chart.component.html',
  styleUrls: ['./export-chart.component.scss']
})
export class ExportChartComponent {
  chart: Chart | undefined = undefined;
  title = 'exported-graph';

  @Input()
  set setChart(chart: Chart) {
    this.chart = chart;
  }

  @Input()
  set setTitle(title: string) {
    this.title = title;
  }

  constructor(private snackBar: MatSnackBar) {
  }

  downloadPdf() {
    const chartElement: HTMLCanvasElement | null = document.querySelector('#canvas');
    if (chartElement) {
      chartElement.toBlob((blob) => {
        if (blob && this.chart) {
          const pdf = new JsPDF('l', 'pt', [chartElement.width, chartElement.height]);
          pdf.addImage(this.chart.toBase64Image(), 0, 0, chartElement.width, chartElement.height);
          pdf.save(this.title + '.pdf');
          this.openSnackBar('File has been successfully downloaded', 'OK');
        }
      });
    } else {
      this.openSnackBar('An error has occurred. Try again.', 'OK');
    }
  }

  downloadCsv() {
    const workbook = new excelJS.Workbook;
    const worksheet = workbook.addWorksheet('Graph data');
    worksheet.columns = [
      {header: 'Label', key: 'label'},
      {header: 'Date', key: 'date'},
      {header: 'Amount', key: 'amount'}
    ];
    if (this.chart && this.chart.data.datasets.length > 0) {
      const labels = this.chart.data.labels;
      this.chart.data.datasets.forEach((dataset) => {
        dataset.data.forEach((data, idx) => {
          worksheet.addRow({label: dataset.label, date: labels ? labels[idx] : 'N/A', amount: data ? data : '0'});
        });
      });
      workbook.xlsx.writeBuffer().then((buffer) => {
        const blob = new Blob([buffer], {type: 'application/xlsx'});
        saveAs(blob, this.title + '.xlsx');
        this.openSnackBar('File has been successfully downloaded', 'OK');
      });
    } else {
      this.openSnackBar('Seems there\'s no data to download. Try a different range.', 'OK');
    }
  }

  openSnackBar(message: string, action: string) {
    this.snackBar.open(message, action, {duration: 3000});
  }
}
