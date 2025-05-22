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

export enum ViewV3 {
  WEEK = 1,
  MONTH = 2,
  YEAR = 3,
  EXPONENTIAL = 4,
  CUSTOM = 5
}

export enum ViewV2 {
  WEEK = 'week',
  MONTH = 'month',
  YEAR = 'year',
  EXPONENTIAL = 'exponential',
  CUSTOM = 'custom'
}

export enum StatValue {
  INSTITUTION = 'institution',
  PIPELINE = 'pipeline',
  WORKSTATION = 'workstation'
}

export enum ChartDataTypes {
  INCREMENTAL = 'incremental',
  EXPONENTIAL = 'exponential'
}

export const defaultView = ViewV2.WEEK; // Weekly fluctuation.

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
