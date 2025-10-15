import {Pipe, PipeTransform} from "@angular/core";

@Pipe({ name: 'sortInternalStatusPipe' })
export class SortInternalStatusPipe implements PipeTransform {
  transform(array: any[], field: string): any[] {
    return array.slice().sort((a, b) => (a[field] > b[field] ? 1 : -1));
  }
}
