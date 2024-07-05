import {Moment} from "moment-timezone";

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
  DATE = 'DATE'
  , NUMBER = 'NUMBER'
  , ENUM = 'ENUM'
  , STRING = 'STRING'
  , LIST = 'LIST'
}

export interface QueryResponse { // response from the backend (maps are a HASSLE to work with in this case, so..)
  id: number;
  query: Query[];
}

export interface QueryView { // collects the individual queries to keep them sorted until they're to be sent to the backend
  node: string;
  property: string;
  fields: QueryInner[];
}

export interface NodeProperty { // I don't like this, it feels redundant, but it was the most "obvious" way to get it to work for now
  node: string;
  property: string;
}

export interface Asset {
  asset_pid: string | undefined;
  asset_guid: string | undefined;
  status: AssetStatus | undefined;
  multi_specimen: boolean | undefined;
  specimens: Specimen[] | undefined;
  funding: string | undefined;
  subject: string | undefined;
  payload_type: string | undefined;
  file_formats: string[] | undefined;
  asset_locked: boolean | undefined;
  restricted_access: string[] | undefined;
  tags: Map<string, string> | undefined;
  audited: boolean | undefined;
  created_date: Moment | undefined;
  date_metadata_updated: Moment | undefined;
  date_asset_taken: Moment | undefined;
  date_asset_deleted: Moment | undefined;
  date_asset_finalised: Moment | undefined;
  date_metadata_taken: Moment | undefined;
  institution: string | undefined;
  parent_guid: string | undefined;
  collection: string | undefined;
  httpInfo: string | undefined;
  internal_status: string | undefined;
  updateUser: string | undefined;
  events: Event[] | undefined;
  digitiser: string | undefined;
  workstation: string | undefined;
  pipeline: string | undefined;
  error_message: string | undefined;
  error_timestamp: Moment | undefined;
}

export interface Specimen {
  institution: string | undefined;
  collection: string | undefined;
  barcode: string | undefined;
  specimen_pid: string | undefined;
  preparation_type: string | undefined;
}

export interface Event {
  user: string | undefined;
  timeStamp: Moment | undefined;
  event: string | undefined;
  pipeline: string | undefined;
  workstation: string | undefined;
}

export interface SavedQuery {
  name: string;
  query: string;
}

export enum AssetStatus {
  WORKING_COPY
  , ARCHIVE
  , BEING_PROCESSED
  , PROCESSING_HALTED
  , ISSUE_WITH_MEDIA
  , ISSUE_WITH_METADATA
  , FOR_DELETION
}

export enum WorkstationStatus {
  IN_SERVICE,
  OUT_OF_SERVICE
}

