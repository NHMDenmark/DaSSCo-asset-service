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
  digitisorName: string;
  createdDate: string;
}

export interface Institute {
  id: number;
  label: string;
  name: string;
  ocrText: string;
  taxonName?: string;
  geographicRegion?: string;
}

export enum TimeFrame {
  WEEK = 7,
  MONTH = 30,
  YEAR = 365
}

