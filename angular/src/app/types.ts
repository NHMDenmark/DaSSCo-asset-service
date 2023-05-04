import moment, {Moment} from 'moment';
import DurationConstructor = moment.unitOfTime.DurationConstructor;

export interface SpecimenGraph {
  instituteName: string;
  pipelineName: string;
  workstationName: string;
  createdDate: string;
  specimens: number;
}

export interface GraphData {
  mainChart?: Map<string, Map<string, number>>;
  subChart?: Map<string, Map<string, number>>;
  labels: string[];
  timeFrame: TimeFrame;
  multi: boolean;
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
  period: 'WEEK' | 'MONTH' | 'YEAR' | 'COMBINEDTOTAL' | 'CUSTOM'; // combinedtotal = totals combined and bar chart w label-specific monthly data
  unit: DurationConstructor;
  format: string;
  startDate: Moment;
  endDate: Moment;
}

export enum StatValue {
  INSTITUTE,
  PIPELINE,
  WORKSTATION
}

export const defaultTimeFrame = 1;

export const MY_FORMATS = {
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
