import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
  name: 'roleRestriction',
  standalone: true
})
export class RoleRestrictionPipe implements PipeTransform {
  transform(value: Record<'name', string>[] | null | undefined): string {
    if (!value || value.length === 0) {
      return '';
    }

    return value
      .map((role) => role.name)
      .filter((name) => !!name)
      .join(', ');
  }
}
