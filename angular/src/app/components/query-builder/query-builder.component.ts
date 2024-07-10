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

  operatorsMap: Map<QueryDataType, string[]> =
    new Map([
      [QueryDataType.STRING, this.operators_string],
      [QueryDataType.NUMBER, this.operators_string],
      [QueryDataType.ENUM, ["="]],
      [QueryDataType.DATE, ['>=', '<=', 'RANGE']],
      [QueryDataType.LIST, ["IN"]]
    ]);

  operators: string[] = [];
  enums: string[] = ['status'];

  @Input() nodes: Map<string, string[]> = new Map<string, string[]>();
  @Input() savedQuery: QueryView | undefined;
  @Output() saveQueryEvent = new EventEmitter<QueryView>();
  @Output() removeComponentEvent = new EventEmitter<any>();

  queryForm = this.fb.group({
    node: new FormControl<NodeProperty | null>(null),
    dataType: new FormControl<QueryDataType | null>(null),
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
        this.setOperatorsAndDataType(choice.property);
      }
    })
  }

  save(childIdx: number | undefined) {
    let innerList: QueryInner[] = [];

    this.wheres.controls.forEach(where => {
      let value;
      // let dataType = QueryDataType.STRING;
      if (this.queryForm.get('dataType')?.value == QueryDataType.DATE) {
        // dataType = QueryDataType.DATE;
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
        dataType: this.queryForm.get('dataType')?.value
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

  setOperatorsAndDataType(property: string) {
    if (property.includes('date') || property.includes('timestamp')) {
      this.queryForm.get('dataType')?.setValue(QueryDataType.DATE);
    } else if (property.includes('file_formats')) { // todo should prob get list names from somewhere
      this.queryForm.get('dataType')?.setValue(QueryDataType.LIST);
    } else if (this.enums.includes(property)) {
      this.queryForm.get('dataType')?.setValue(QueryDataType.ENUM);
      this.wheres.controls.forEach(where => where.get('operator')?.setValue(this.operatorsMap.get(QueryDataType.ENUM)![0]))
    } else {
      this.queryForm.get('dataType')?.setValue(QueryDataType.STRING);
    }
    const dataType = this.queryForm.get('dataType')?.value;
    this.operators = this.operatorsMap.get(dataType ? QueryDataType[dataType] : QueryDataType.STRING)!; // I'M SORRY i know ! is bad, but I KNOW it will always have STRING key. it is hardcoded goddammit.
  }

  addWhereData(where: QueryInner) {
    const isDate = where.dataType == QueryDataType.DATE;
    const rangedDate = isDate && where.operator.toLowerCase().includes('range');
    this.queryForm.get('dataType')?.setValue(QueryDataType[where.dataType]);

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
      operator: new FormControl(this.queryForm.get('dataType')?.value == QueryDataType.ENUM ? '=' : null, Validators.required),
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

  protected readonly QueryDataType = QueryDataType;
}
