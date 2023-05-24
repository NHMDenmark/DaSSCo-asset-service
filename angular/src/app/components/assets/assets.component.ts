import {Component, OnInit} from '@angular/core';
import {Asset} from "../domain/asset";
import {Temporal} from "@js-temporal/polyfill";


const asset: Asset = {
  originalMedia: 'CP0002637_L_selago_Fuji_ICC',
  originalMediaTaken: Temporal.Instant.fromEpochMilliseconds(Date.now()),
  digitiser: 'Justin Hungerford',
  workstationName: 'WORKHERB0001',
  pipelineName: 'PIPEHERB0001',
  institution: 'NHMD',
  collection: 'Entomology',
  dateMediaCreated: Temporal.Instant.fromEpochMilliseconds(Date.now()),
  mediaCreatedBy: 'PIPEHERB0001',
  dateMediaUpdated: [
    Temporal.Instant.fromEpochMilliseconds(Date.now()),
  ],
  mediaUpdatedBy: [
    'PIPEHERB0001'
  ],
  dateMediaDeleted: '',
  mediaDeletedBy: '',
  dateMetadataCreated: Temporal.Instant.fromEpochMilliseconds(Date.now()),
  metadataCreatedBy: [
    'PIPEHERB0001'
  ],
  dateMetadataUpdated: [
    Temporal.Instant.fromEpochMilliseconds(Date.now()),
  ],
  metadataUpdatedBy: [
    'PIPEHERB0001'
  ],
  audited: 'yes',
  auditedBy: 'Chelsea Graham',
  auditedDate: Temporal.Instant.fromEpochMilliseconds(Date.now()),
  status: 'archive',
  storageLocation: '',
  parent: '',
  originalParent: '',
  relatedMedia: '7e7-1-02-11-21-25-1-01-001-05a8c7-00000000',
  mutispecimenStatus: 'no',
  otherMultispecimen: '',
  barcode: 'CP0002637',
  specimenPid: '',
  specifySpecimenId: 'ae1fcf25-7e94-4506-8d64-5c54d69fa900',
  specifyAttachmentId: 'b33ea887-11ab-43b9-a562-44fdfe32af8e',
  mediaGuid: '7e7-1-02-11-21-25-1-01-001-05a8cb-00000000',
  mediaPid: '',
  externalLink: '',
  payloadType: 'image',
  fileFormat: 'tif',
  fileInfo: '',
  accessLevel: '',
  preparationType: '',
  ocrText: 'FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.',
  geographicRegion: '',
  taxonName: '',
  typeStatus: '',
  specimenStorageLocation: '',
  funding: '',
  copyrightOwner: 'NHMD',
  license: 'Attribution 4.0 International (CC BY 4.0)',
  embargoType: '',
  embargoNotes: '',
  equipmentDetails: [],
  exposureTime: '',
  fNumber: '',
  focalLength: '',
  isoSetting: '',
  whiteBalance: '',
  originalSpecifyMediaName: 'https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928',
  mediaSubject: 'specimen',
  notes: [],
  pushAssetToSpecify: 'no',
  pushMetadataToSpecify: 'yes'
}

@Component({
  selector: 'dassco-assets',
  templateUrl: './assets.component.html',
  styleUrls: ['./assets.component.scss']
})
export class AssetsComponent implements OnInit {
  assets: Asset[] = [];
  value: string = '';

  constructor() {
    for (let i = 0; i < 25; i++) {
      this.assets.push(asset)
    }
  }

  ngOnInit(): void {
  }

}
