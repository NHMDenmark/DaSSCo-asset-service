import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Query, QueryField} from "../../types";
import {FormArray, FormBuilder, FormControl, Validators} from "@angular/forms";
import {BehaviorSubject} from "rxjs";
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

  chosenNodePropertiesSubject = new BehaviorSubject<string[]>([]);
  public chosenNodeProperties$ = this.chosenNodePropertiesSubject.asObservable();
  chosenNode: string | undefined;
  saved: boolean = false;
  @Input() nodes: Map<string, string[]> = new Map<string, string[]>();
  @Output() saveQueryEvent = new EventEmitter<Query>();
  @Output() removeComponentEvent = new EventEmitter<any>();

  queryForm = this.fb.group({
    wheres: this.fb.array([
      this.fb.group({
        queryType: new FormControl('and', Validators.required),
        property: new FormControl('', Validators.required),
        operator: new FormControl('', Validators.required),
        value: new FormControl('', Validators.required),
        date: new FormControl<Date | null>(null),
        dateStart: new FormControl<Date | null>(null),
        dateEnd: new FormControl<Date | null>(null),
        operators: new FormControl(this.operators_string),
        isDate: new FormControl<boolean>(false),
      })
    ])
  });

  get wheres() {
    return this.queryForm.get('wheres') as FormArray;
  }

  constructor(private fb: FormBuilder) {
    this.queryForm.get('wheres')?.valueChanges.subscribe(() => {
      if (this.saved) {
        this.saved = false;
      }
    });
  }

  ngOnInit(): void {
  }

  save() {
    let whereList: QueryField[] = [];

    this.wheres.controls.forEach(where => {
      let value;
      if (where.get('isDate')?.value) {
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
        type: where.get('queryType')?.value,
        operator: where.get('operator')?.value,
        property: where.get('property')?.value,
        value: value
      } as QueryField;
      whereList.push(newQueryField);
    })

    this.saved = true;
    this.saveQueryEvent.emit({select: this.chosenNode, where: whereList});
  }

  removeComponent() {
    this.removeComponentEvent.emit();
  }

  chooseNode(choice: string) {
    if (this.nodes.has(choice)) {
      this.chosenNodePropertiesSubject.next(this.nodes.get(choice)!);
    }
  }

  updateOperators(index: number) {
    const propertyValue = this.wheres.at(index).get('property')?.value;
    let newOperators = this.operators_string;  // Default operators list
    this.wheres.at(index).get('isDate')?.setValue(false);

    if (propertyValue.includes('date') || propertyValue.includes('timestamp')) {
      this.wheres.at(index).get('isDate')?.setValue(true);
      newOperators = this.operators_date;
    } else if (propertyValue.includes('file_formats')) {
      newOperators = this.operators_list;
    }
    this.wheres.at(index).get('operators')?.setValue(newOperators);
  }

  addWhere(type: string) {
    this.wheres.push(this.fb.group({
      queryType: new FormControl(type, Validators.required),
      property: new FormControl('', Validators.required),
      operator: new FormControl('', Validators.required),
      value: new FormControl(''),
      date: new FormControl<Date | null>(null),
      dateStart: new FormControl<Date | null>(null),
      dateEnd: new FormControl<Date | null>(null),
      operators: new FormControl(this.operators_string),
      isDate: new FormControl<boolean>(false),
    }));
  }

  removeWhere(index: number) {
    this.wheres.removeAt(index);
  }
}
