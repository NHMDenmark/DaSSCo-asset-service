export interface Query {
  select: string | undefined;
  where: QueryWhere[];
}

export interface QueryWhere {
  property: string;
  fields: QueryInner[];
}

export interface QueryInner {
  operator: string;
  value: string;
  dataType: QueryDataType;
}

export enum QueryDataType {
  DATE = 'DATE',
  NUMBER = 'NUMBER',
  ENUM = 'ENUM',
  STRING = 'STRING',
  LIST = 'LIST',
  BOOLEAN = 'BOOLEAN'
}

export interface QueryResponse {
  // response from the backend (maps are a HASSLE to work with in this case, so..)
  id: number;
  query: Query[];
}

export interface QueryView {
  // collects the individual queries to keep them sorted until they're to be sent to the backend
  node: string;
  property: string;
  fields: QueryInner[];
}

export interface NodeProperty {
  // I don't like this, it feels redundant, but it was the most "obvious" way to get it to work for now
  node: string;
  property: string;
}

export interface SavedQuery {
  name: string;
  query: string;
}

export enum WorkstationStatus {
  IN_SERVICE,
  OUT_OF_SERVICE
}
