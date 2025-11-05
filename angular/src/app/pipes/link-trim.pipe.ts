import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
  name: 'linkTrim',
  standalone: true // optional if using standalone components in Angular 14+
})
export class LinkTrimPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }

    // Remove "https://" or "http://", and any leading slash
    return value.replace(/^https?:\/\//i, '').replace(/^\/+/, '');
  }
}
