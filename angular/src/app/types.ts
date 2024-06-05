import {Moment} from "moment-timezone";

export interface GraphStatsV2 {
  institutes: Map<string, number>;
  pipelines: Map<string, number>;
  workstations: Map<string, number>;
}

export interface Institute {
  id: number;
  label: string;
  name: string;
  ocrText: string;
  taxonName?: string;
  geographicRegion?: string;
}

export interface InternalStatusDataSource {
  status: 'COMPLETED' | 'PENDING' | 'FAILED';
  no: number;
}

export enum ViewV2 {
  WEEK = 1,
  MONTH = 2,
  YEAR = 3,
  EXPONENTIAL = 4
}

export enum StatValue {
  INSTITUTE,
  PIPELINE,
  WORKSTATION
}

export enum ChartDataTypes {
  INCREMENTAL = 'incremental',
  EXPONENTIAL = 'exponential'
}

export const defaultView = 1; // Weekly fluctuation.

export interface Query {
  select: string | undefined;
  wheres: QueryField[];
}

export interface QueryField {
  type: 'and' | 'or';
  operator: string;
  property: string;
  value: string;
}

export interface Asset {
  asset_pid: string | undefined;
  asset_guid: string | undefined;
  status: AssetStatus | undefined;
  multi_specimen: boolean | undefined;
  specimens: any[] | undefined;
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
  events: any[] | undefined;
  digitiser: string | undefined;
  workstation: string | undefined;
  pipeline: string | undefined;
  error_message: string | undefined;
  error_timestamp: Moment | undefined;
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

export const CUSTOM_DATE_FORMAT = {
  parse: {
    dateInput: 'DD-MM-YYYY'
  },
  display: {
    dateInput: 'DD-MM-YYYY',
    monthYearLabel: 'MMM YYYY',
    dateA11yLabel: 'DD-MM-YYYY',
    monthYearA11yLabel: 'MMM YYYY'
  }
};
