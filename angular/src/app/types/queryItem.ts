export interface QueryItem{
  name: string;
  properties: QueryProperty[]
}

export interface QueryProperty{
  name: string;
  dataType: string;
  parent: string;
}
