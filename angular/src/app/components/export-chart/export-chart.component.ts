import {Component, Input} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';

@Component({
  selector: 'dassco-export-chart',
  templateUrl: './export-chart.component.html',
  styleUrls: ['./export-chart.component.scss']
})
export class ExportChartComponent {
  chartSubject = new BehaviorSubject<string>('');

  @Input()
  set setChart(chart: any) {
    this.chartSubject.next(chart);
  }

  test() {
    const chartElement = document.querySelector('#canvas');
    if (chartElement) {
      html2canvas(chartElement as HTMLElement).then(canvas => {
        const pdfFile = new jsPDF('l', 'pt', [canvas.width, canvas.height]);
        const imgData = canvas.toDataURL('image/png', 1);
        pdfFile.addImage(imgData, 0, 0, canvas.width, canvas.height);
        pdfFile.save('sample.pdf');
      });
    }
  }
}
