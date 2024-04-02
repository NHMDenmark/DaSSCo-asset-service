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
  status: string;
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
