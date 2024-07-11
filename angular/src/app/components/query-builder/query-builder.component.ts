import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {NodeProperty, QueryDataType, QueryInner, QueryView} from "../../types/query-types";
import {FormArray, FormBuilder, FormControl, Validators} from "@angular/forms";
import {Moment} from "moment-timezone";
import {BehaviorSubject, map, Observable} from "rxjs";
import {CacheService} from "../../services/cache.service";

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

  enumOverview: {cacheName: string, node: NodeProperty}[] =
    [{cacheName: 'institutions', node: {node: 'Institution', property: 'name'}},
    {cacheName: 'pipelines', node: {node: 'Pipeline', property: 'name'}},
    {cacheName: 'collections', node: {node: 'Collection', property: 'name'}},
    {cacheName: 'workstations', node: {node: 'Workstation', property: 'name'}},
    {cacheName: 'digitisers', node: {node: 'User', property: 'name'}},
    {cacheName: 'payload_types', node: {node: 'Asset', property: 'payload_type'}},
    {cacheName: 'preparation_types', node: {node: 'Specimen', property: 'preparation_type'}},
    {cacheName: 'restricted_access', node: {node: 'Asset', property: 'restricted_access'}},
    {cacheName: 'status', node: {node: 'Asset', property: 'status'}},
    {cacheName: 'subjects', node: {node: 'Asset', property: 'subject'}}];

  // enums: NodeProperty[] = [
  //   {node: 'Workstation', property: 'name'},
  //   {node: 'Pipeline', property: 'name'},
  //   {node: 'Collection', property: 'name'},
  //   {node: 'User', property: 'name'},
  //   {node: 'Asset', property: 'payload_type'},
  //   {node: 'Asset', property: 'restricted_access'},
  //   {node: 'Asset', property: 'subject'},
  //   {node: 'Asset', property: 'status'},
  //   {node: 'Specimen', property: 'preparation_type'}
  // ]

  dropdownValueMap: Map<string, object[]> | undefined = new Map();

  selectedEnumValues = [''];
  filteredEnumValues = new BehaviorSubject<string[]>(['']);

  enumPropertiesSubject = new BehaviorSubject<string[]>(['']);

  cachedDropdownValues$: Observable<Map<string, object[]> | undefined>
    = this.cacheService.cachedDropdownValues$
      .pipe(
        map(values => {
          if (values) {
            this.dropdownValueMap = new Map(Object.entries(values));
          }
          console.log(values)
          return values;
        })
      );

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
        this.chosenNode.setValue({node: this.savedQuery.node, property: this.savedQuery.property});
        this.savedQuery.fields.forEach(whereField => {
          this.addWhereData(whereField);
        })
      this.save(undefined);
    }
  }

  constructor(private fb: FormBuilder
            , private cacheService: CacheService) {
    this.chosenNode.valueChanges.subscribe((choice: NodeProperty | null) => {
      if (choice) {
        if (!this.wheres.pristine) {
          if (this.wheres.length > 1) { // reset() can't be used if there's multiple elements in the form array
            this.wheres.clear();
          } else {
            this.wheres.reset();
          }
        }
        this.setOperatorsAndDataType(choice);
      }
    })
  }

  save(childIdx: number | undefined) {
    let innerList: QueryInner[] = [];

    this.wheres.controls.forEach(where => {
      let value;
      if (this.queryForm.get('dataType')?.value == QueryDataType.DATE) {
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

  setOperatorsAndDataType(nodeProperty: NodeProperty) {
    const isEnum = this.enumOverview.find(enumNode => enumNode.node.node == nodeProperty.node && enumNode.node.property == nodeProperty.property);
    console.log(isEnum)
    console.log(nodeProperty)
    if (isEnum) {
      console.log('in enum!')
      if (this.dropdownValueMap) {
        const values = this.dropdownValueMap.get(isEnum.cacheName);
        console.log(this.dropdownValueMap)
        console.log(this.dropdownValueMap.get(isEnum.cacheName));
        if (values) {
          console.log(Object.keys(values));
          this.selectedEnumValues = Object.keys(values);
        }
        // this.selectedEnumValues = this.dropdownValueMap.get(isEnum.cacheName);
      }
      this.filteredEnumValues.next(this.selectedEnumValues);
      this.queryForm.get('dataType')?.setValue(QueryDataType.ENUM);
      this.wheres.controls.forEach(where => where.get('operator')?.setValue(this.operatorsMap.get(QueryDataType.ENUM)![0]))

    } else if (nodeProperty.property.includes('date') || nodeProperty.property.includes('timestamp')) {
      this.queryForm.get('dataType')?.setValue(QueryDataType.DATE);
    } else if (nodeProperty.property.includes('file_formats')) { // todo should prob get list names from somewhere
      this.queryForm.get('dataType')?.setValue(QueryDataType.LIST);
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

  filterAutocomplete(value: any) {
    this.filteredEnumValues.next(this.selectedEnumValues.filter(enumVal => enumVal.includes(value.toLowerCase())))
  }

  protected readonly QueryDataType = QueryDataType;
}
