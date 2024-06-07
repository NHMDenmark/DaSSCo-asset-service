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
        operators: new FormControl(this.operators_string)
      })
    ])
  });

  get wheres() {
    return this.queryForm.get('wheres') as FormArray;
  }

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    // console.log(this.nodes)
    // this.queryForm.get('wheres')?.valueChanges.subscribe(value => {
    //   value.forEach((_group: any, index: number) => {
    //     const propertyControl = this.wheres.at(index).get('property');
    //     propertyControl?.valueChanges.subscribe((propertyValue: string) => {
    //       this.updateOperators(propertyValue, index);
    //     });
    //   });
    // });
  }

  save() {
    console.log(this.wheres.controls)
    let whereList: QueryField[] = [];

    this.wheres.controls.forEach(where => {

      let value;
      if (where.get('property') && where.get('property')?.value.includes('date') || where.get('property')?.value.includes('timestamp')) {
        // console.log('property is date')
        if (where.get('dateStart') && where.get('dateEnd')) {
          // console.log('date start and end are there')
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
      // console.log(value)

      const newQueryField = {
        type: where.get('property')?.value == 'creation_date' && where.get('queryType')?.value == 'Asset' ? 'Event' :where.get('queryType')?.value,
        operator: where.get('operator')?.value,
        property: where.get('property')?.value,
        value: value
      } as QueryField;
      whereList.push(newQueryField);
      // console.log(newQueryField)
    })
    this.saveQueryEvent.emit({select: this.chosenNode, wheres: whereList});
  }

  removeComponent() {
    this.removeComponentEvent.emit();
  }

  chooseNode(choice: string) {
    if (this.nodes.has(choice)) {
      // console.log(this.nodes.get(choice))
      this.chosenNodePropertiesSubject.next(this.nodes.get(choice)!);
    }
  }

  updateOperators(index: number) {
    const propertyValue = this.wheres.at(index).get('property')?.value;
    let newOperators = this.operators_string;  // Default operators list
    if (propertyValue.includes('date') || propertyValue.includes('timestamp')) {
      newOperators = this.operators_date;
    } else if (propertyValue.includes('file_formats')) {
      newOperators = this.operators_list;
    }
    // Update the operators control
    // console.log(newOperators)
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
      operators: new FormControl(this.operators_string)
    }));
  }

  removeWhere(index: number) {
    this.wheres.removeAt(index);
  }
}
