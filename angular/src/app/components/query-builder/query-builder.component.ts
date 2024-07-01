import {Component, EventEmitter, Input, Output} from '@angular/core';
import {NodeProperty, QueryInner, QueryView} from "../../types/query-types";
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
  chosenNodePropertySubject = new BehaviorSubject<NodeProperty | undefined>(undefined);
  chosenNodeProperty$ = this.chosenNodePropertySubject.asObservable();
  isDate = false;

  @Input()
  set jsonForm(json: string | undefined) {
    if (json) {
      console.log(json)
      console.log(JSON.parse(json))
      const wheres = JSON.parse(json).wheres;
      // this.queryForm.patchValue(JSON.parse(json));
      this.queryForm.controls.node.patchValue(JSON.parse(json).node);
      this.wheres.clear();
      wheres.forEach((where: any) => {
        console.log(where)
        this.wheres.push(this.fb.group(where));
        // console.log(where)
        // console.log(JSON.parse(where))
        // this.wheres.push(where)
      })

      // this.queryForm.patchValue({node: JSON.parse(json).node, wheres: JSON.parse(json).wheres});
      // this.wheres.patchValue(JSON.parse(json).wheres)
      // console.log(JSON.parse(json).wheres)
      // this.queryForm.patchValue(JSON.parse(json));
    }
  }

  @Input() nodes: Map<string, string[]> = new Map<string, string[]>();
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

  constructor(private fb: FormBuilder) {
    this.chosenNode.valueChanges.subscribe(choice => {
      if (choice) {
        if (!this.wheres.pristine) {
          if (this.wheres.length > 1) { // reset() can't be used if there's multiple elements in the form array
            this.wheres.clear();
            console.log('hudshfjkdshkj')
            this.addWhere();
          } else {
            this.wheres.reset();
          }
        }
        this.chosenNodePropertySubject.next(choice);
        this.updateOperators(choice.property);
      }
    })
  }

  save(childIdx: number | undefined) {
    let innerList: QueryInner[] = [];

    this.wheres.controls.forEach(where => {
      let value;

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
    })

    if (childIdx != undefined) this.wheres.at(childIdx).markAsUntouched();
    localStorage.setItem('form', JSON.stringify(this.queryForm.value));

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
