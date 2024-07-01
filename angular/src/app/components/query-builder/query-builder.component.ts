import {Component, EventEmitter, Input, Output} from '@angular/core';
import {QueryInner, QueryView} from "../../types/query-types";
import {FormArray, FormBuilder, FormControl, Validators} from "@angular/forms";
import {BehaviorSubject} from "rxjs";
import {Moment} from "moment-timezone";

@Component({
  selector: 'dassco-query-builder',
  templateUrl: './query-builder.component.html',
  styleUrls: ['./query-builder.component.scss']
})
export class QueryBuilderComponent {
  operators_string = [
    '=',
    'STARTS WITH',
    'ENDS WITH',
    'CONTAINS'
  ]
  operators_date = [
    '>=',
    '<=',
    'RANGE'
  ]
  operators_list = [ // file_formats in Asset is currently the only list
    'IN'
  ]
  operators: string[] = [];
  chosenNodePropertySubject = new BehaviorSubject<{ node: string, property: string } | undefined>(undefined);
  chosenNodeProperty$ = this.chosenNodePropertySubject.asObservable();
  isDate = false;

  @Input() nodes: Map<string, string[]> = new Map<string, string[]>();
  @Output() saveQueryEvent = new EventEmitter<QueryView>();
  @Output() removeComponentEvent = new EventEmitter<any>();

  nodeControl = new FormControl({} as { node: string, property: string });

  queryForm = this.fb.group({
    wheres: this.fb.array([
      this.fb.group({
        operator: new FormControl(null, Validators.required),
        value: new FormControl(null, Validators.required),
        date: new FormControl<Date | null>(null),
        dateStart: new FormControl<Date | null>(null),
        dateEnd: new FormControl<Date | null>(null)
      })
    ])
  });

  get wheres() {
    return this.queryForm.get('wheres') as FormArray;
  }

  constructor(private fb: FormBuilder) {
    this.nodeControl.valueChanges.subscribe((nodeChoice) => {
      if (nodeChoice) {
        if (!this.wheres.pristine) {
          if (this.wheres.length > 1) { // reset() can't be used if there's multiple elements in the form array
            this.wheres.clear();
            this.addWhere();
          } else {
            this.wheres.reset();
          }
        }
        this.chosenNodePropertySubject.next(nodeChoice);
        this.updateOperators(nodeChoice.property);
      }
    })
  }

  save(childIdx: number) {
    let innerList: QueryInner[] = [];
    let node = this.nodeControl.value?.node;

    this.wheres.controls.forEach(where => {
      let value;

      console.log(where.get('isDate')?.value)
      if (this.isDate) {
        if (where.get('operator')?.value == 'RANGE') {
          const dateStart = <Moment>where.get('dateStart')?.value;
          const dateEnd = <Moment>where.get('dateEnd')?.value;
          value = dateStart.valueOf() + '#' + dateEnd.valueOf();
        } else {
          const date = <Moment>where.get('date')?.value;
          value = date.valueOf();
        }
      } else {
        value = where.get('value')?.value;
      }

      const newQueryField = {
        operator: where.get('operator')?.value,
        value: value
      } as QueryInner;
      innerList.push(newQueryField);
      console.log(where)
    })

    this.wheres.at(childIdx).markAsUntouched();
    this.saveQueryEvent.emit({
      node: node ? node : '',
      property: this.nodeControl.value ? this.nodeControl.value.property : '',
      fields: innerList
    });
  }

  removeComponent() {
    this.removeComponentEvent.emit();
  }

  updateOperators(property: string) {
    this.operators = this.operators_string;  // Default operators list
    if (property.includes('date') || property.includes('timestamp')) {
      this.isDate = true;
      this.operators = this.operators_date;
    } else if (property.includes('file_formats')) {
      this.isDate = false;
      this.operators = this.operators_list;
    }
  }

  addWhere() {
    this.wheres.push(this.fb.group({
      operator: new FormControl(null, Validators.required),
      value: new FormControl(null, Validators.required),
      date: new FormControl<Date | null>(null),
      dateStart: new FormControl<Date | null>(null),
      dateEnd: new FormControl<Date | null>(null)
    }));
  }

  removeWhere(index: number): void {
    if (index == 0 && this.wheres.length <= 1) {
      this.wheres.at(index).reset();
    } else {
      this.wheres.removeAt(index);
    }
  }
}
