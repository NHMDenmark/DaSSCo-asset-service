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
