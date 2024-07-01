import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {QueryInner, QueryView} from "../../types/query-types";
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
  operators: string[] = [];
  chosenNodePropertySubject = new BehaviorSubject<{node: string, property: string} | undefined>(undefined);
  chosenNodeProperty$ = this.chosenNodePropertySubject.asObservable();

  @Input() nodes: Map<string, string[]> = new Map<string, string[]>();
  @Output() saveQueryEvent = new EventEmitter<QueryView>();
  @Output() removeComponentEvent = new EventEmitter<any>();

  nodeControl = new FormControl({} as {node: string, property: string});

  queryForm = this.fb.group({
    wheres: this.fb.array([
      this.fb.group({
        operator: new FormControl(null, Validators.required),
        value: new FormControl(null, Validators.required),
        date: new FormControl<Date | null>(null),
        dateStart: new FormControl<Date | null>(null),
        dateEnd: new FormControl<Date | null>(null),
        isDate: new FormControl<boolean>(false),
      })
    ])
  });

  get wheres() {
    return this.queryForm.get('wheres') as FormArray;
  }

  constructor(private fb: FormBuilder) {
    // this.queryForm.get('wheres')?.valueChanges.subscribe(() => {
    //   console.log(this.queryForm)
    //   // if (this.saved) {
    //   //   this.saved = false;
    //   // }
    // });

    this.nodeControl.valueChanges.subscribe((nodeChoice) => {
      console.log(nodeChoice)
      if (nodeChoice) {
        this.chosenNodePropertySubject.next(nodeChoice);
        this.updateOperators(nodeChoice.property);
      }
    })
  }

  ngOnInit(): void {
  }

  save(childIdx: number) {
    let innerList: QueryInner[] = [];
    let node = this.nodeControl.value?.node;

    this.wheres.controls.forEach(where => {
      let value;
      if (where.get('property')?.value.includes('date') || where.get('property')?.value.includes('timestamp')) {
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

      console.log(this.nodeControl)

      const newQueryField = {
        operator: where.get('operator')?.value,
        value: value
      } as QueryInner;
      innerList.push(newQueryField);
      console.log(where)
    })

    console.log(innerList);

    this.wheres.at(childIdx).markAsUntouched();
    this.saveQueryEvent.emit({node: node ? node : '',
      property: this.nodeControl.value ? this.nodeControl.value.property : '',
      fields: innerList
    });
  }

  removeComponent() {
    this.removeComponentEvent.emit();
  }

  updateOperators(property: string) {
    // const propertyValue = this.wheres.at(index).get('property')?.value;
    this.operators = this.operators_string;  // Default operators list
    // this.wheres.at(index).get('isDate')?.setValue(false);
    if (property.includes('date') || property.includes('timestamp')) {
      this.operators = this.operators_date;
    } else if (property.includes('file_formats')) {
      this.operators = this.operators_list;
    }
  }

  addWhere() {
    this.wheres.push(this.fb.group({
      // queryType: new FormControl(type, Validators.required),
      // property: new FormControl('', Validators.required),
      operator: new FormControl(null, Validators.required),
      value: new FormControl(null, Validators.required),
      date: new FormControl<Date | null>(null),
      dateStart: new FormControl<Date | null>(null),
      dateEnd: new FormControl<Date | null>(null),
      isDate: new FormControl<boolean>(false),
    }));
  }

  removeWhere(index: number) {
    this.wheres.removeAt(index);
  }
}
