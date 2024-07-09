import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {NodeProperty, QueryDataType, QueryInner, QueryView} from "../../types/query-types";
import {FormArray, FormBuilder, FormControl, Validators} from "@angular/forms";
import {Moment} from "moment-timezone";

@Component({
  selector: 'dassco-query-builder',
  templateUrl: './query-builder.component.html',
  styleUrls: ['./query-builder.component.scss']
})
export class QueryBuilderComponent implements OnInit {
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
  isDate = false;

  @Input() nodes: Map<string, string[]> = new Map<string, string[]>();
  @Input() savedQuery: QueryView | undefined;
  @Output() saveQueryEvent = new EventEmitter<QueryView>();
  @Output() removeComponentEvent = new EventEmitter<any>();

  queryForm = this.fb.group({
    node: new FormControl<NodeProperty | null>(null),
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

  get chosenNode() {
    return this.queryForm.get('node') as FormControl;
  }

  ngOnInit(): void {
    if (this.savedQuery) {
      this.wheres.clear();
      // Array.from(this.savedQuery.where).forEach(where => {
        this.chosenNode.setValue({node: this.savedQuery.node, property: this.savedQuery.property});
        this.savedQuery.fields.forEach(whereField => {
          this.addWhereData(whereField);
        })
      // })
      this.save(undefined);
    }
  }

  constructor(private fb: FormBuilder) {
    this.chosenNode.valueChanges.subscribe(choice => {
      if (choice) {
        if (!this.wheres.pristine) {
          if (this.wheres.length > 1) { // reset() can't be used if there's multiple elements in the form array
            this.wheres.clear();
          } else {
            this.wheres.reset();
          }
        }
        this.updateOperators(choice.property);
      }
    })
  }

  save(childIdx: number | undefined) {
    let innerList: QueryInner[] = [];

    this.wheres.controls.forEach(where => {
      let value;
      let dataType = QueryDataType.STRING;
      if (this.isDate) {
        dataType = QueryDataType.DATE;
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
        value: value,
        dataType: dataType
      } as QueryInner;
      innerList.push(newQueryField);
    })
    if (childIdx != undefined) this.wheres.at(childIdx).markAsUntouched();

    this.saveQueryEvent.emit({
      node: this.chosenNode.value.node,
      property: this.chosenNode.value.property,
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

  addWhereData(where: QueryInner) {
    const isDate = where.dataType == QueryDataType.DATE;
    const rangedDate = isDate && where.operator.toLowerCase().includes('range');

    this.wheres.push(this.fb.group({
      operator: new FormControl(where.operator, Validators.required),
      value: new FormControl(!isDate ? where.value : null, Validators.required),
      date: new FormControl<Date | null>(isDate && !rangedDate ? new Date(Date.parse(where.value)) : null),
      dateStart: new FormControl<Date | null>(rangedDate ? new Date(Date.parse(where.value.split('#')[0])) : null),
      dateEnd: new FormControl<Date | null>(rangedDate ? new Date(Date.parse(where.value.split('#')[1])) : null)
    }));
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
    this.save(undefined);
  }

  compareNodeProperty(o1: any, o2: any): boolean {
    return !!(o1 && o2 && o1.node == o2.node && o1.property == o2.property);
  }
}
