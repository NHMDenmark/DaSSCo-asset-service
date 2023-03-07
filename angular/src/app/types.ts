import moment from "moment";
import DurationConstructor = moment.unitOfTime.DurationConstructor;

export interface SpecimenGraph {
  instituteName: string;
  instituteOcrText: string;
  specimenName: string;
  specimenMediaSubject: string;
  specimenSpecifySpecId: string; // spec == specimen
  specimenSpecifyAttId: string; // att = attachment
  specimenOrigSpecifyMediaName: string;
  assetName: string;
  assetMediaGuid: string;
  assetFileFormat: string;
  assetDateMediaCreated: string;
  pipelineName: string;
  workstationName: string;
  createdDate: string;
}

export interface GraphData {
  lineChart: Map<string, Map<string, number>>;
  barChart?: Map<string, Map<string, number>>;
}

export interface Institute {
  id: number;
  label: string;
  name: string;
  ocrText: string;
  taxonName?: string;
  geographicRegion?: string;
}

export interface TimeFrame {
  period: 'WEEK' | 'MONTH' | 'YEAR';
  amount: number;
  unit: DurationConstructor;
  format: string;
}

export const defaultTimeFrame: TimeFrame = {period: 'WEEK', amount: 7, unit: 'days', format: 'DD-MMM-YY'};

export enum StatValue {
  INSTITUTE,
  PIPELINE,
  WORKSTATION
}
