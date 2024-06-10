import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';

@Component({
  selector: 'dassco-bulk-update',
  templateUrl: './bulk-update.component.html',
  styleUrls: ['./bulk-update.component.scss']
})
export class BulkUpdateComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

  // TAGS:
  tags: { key: string, value: string }[] = [];
  newTag: string = '';
  newDescription: string = '';

  // SPECIMEN:
  specimens: { barcode: string, specimen_pid: string, preparation_type: string}[] = [];
  newBarcode: string = '';
  newSpecimenPid: string = "";
  newPrepType: string = "";

  // ASSET STATUS:
  statusList = ["WORKING_COPY", "ARCHIVE", "BEING_PROCESSED", "PROCESSING_HALTED", "ISSUE_WITH_MEDIA", "ISSUE_WITH_METADATA", "FOR_DELETION"];
  status: string = "";

  // ASSET_LOCKED:
  assetLocked: boolean = false;

  // SUBJECT:
  subject: string = "";

  // RESTRICTED_ASSET
  roles: string[] = ["USER", "ADMIN", "SERVICE_USER", "DEVELOPER"]
  roleList: string[] = [];

  // FUNDING:
  funding: string = "";

  // FILE_FORMAT:
  formats: string[] = ["TIF", "JPEG", "RAW", "RAF", "CR3", "DNG", "TXT"]
  formatList: string[] = [];

  // PAYLOAD_TYPE
  payloadType: string = "";

  // PARENT_GUID
  parentGuid: string = "";

  @ViewChild('tagInput') tagInput!: ElementRef<HTMLInputElement>
  @ViewChild('barcodeInput') barcodeInput!: ElementRef<HTMLInputElement>

  modifyFileFormatList(event: any, format: string){
    if (event.checked){
      this.formatList.push(format);
    } else {
      const index = this.formatList.indexOf(format);
      if (index >= 0){
        this.formatList.splice(index, 1);
      }
    }
  }

  modifyRoleList(event: any, role: string){
    if(event.checked){
      this.roleList.push(role);
    } else {
      const index = this.roleList.indexOf(role)
      if (index >= 0 ){
        this.roleList.splice(index, 1);
      }
    }
  }

  add(event: any): void {
    event.preventDefault();
    if (this.newTag.trim()) {
      this.tags.push({ key: this.newTag.trim(), value: this.newDescription.trim() });
      this.newTag = '';
      this.newDescription = '';

      this.tagInput.nativeElement.focus();
    }
  }

  addSpecimen(event: any): void {
    event.preventDefault();
    if (this.newBarcode.trim()) {
      this.specimens.push({ barcode: this.newBarcode.trim(), specimen_pid: this.newSpecimenPid.trim(), preparation_type: this.newPrepType.trim()});
      this.newBarcode = "";
      this.newSpecimenPid = "";
      this.newPrepType = "";

      this.barcodeInput.nativeElement.focus();
    }
  }

  remove(pair: { key: string, value: string }): void {
    const index = this.tags.indexOf(pair);
    if (index >= 0) {
      this.tags.splice(index, 1);
    }
  }

  removeSpecimen(specimen: {barcode: string, specimen_pid: string, preparation_type: string}): void {
    const index = this.specimens.indexOf(specimen);
    if (index >= 0){
      this.specimens.splice(index, 1);
    }
  }

  showJson(){
    const tagsObject: {[key : string]: string} = {};

    this.tags.forEach(pair => {
      tagsObject[pair.key] = pair.value;
    })

    const json = JSON.stringify({
      status: this.status,
      tags: tagsObject,
      specimens: this.specimens,
      asset_locked: this.assetLocked,
      subject: this.subject,
      restricted_asset: this.roleList,
      funding: this.funding,
      file_formats: this.formatList,
      payload_type: this.payloadType,
      parent_guid: this.parentGuid
    }, null, 2)
    console.log(json);
  }

}
