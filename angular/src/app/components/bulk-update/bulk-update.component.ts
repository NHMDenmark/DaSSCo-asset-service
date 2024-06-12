import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {BulkUpdateService} from "../../services/bulk-update.service";
import {MatSnackBar, MatSnackBarDismiss} from "@angular/material/snack-bar";
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {Observable} from "rxjs";

@Component({
  selector: 'dassco-bulk-update',
  templateUrl: './bulk-update.component.html',
  styleUrls: ['./bulk-update.component.scss']
})
export class BulkUpdateComponent implements OnInit {

  constructor(private bulkUpdateService: BulkUpdateService,
              private _snackBar: MatSnackBar) { }

  ngOnInit(): void {
  }

  // TAGS:
  tags: { key: string, value: string }[] = [];
  newTag: string = '';
  newDescription: string = '';
  submitted: boolean = false;
  @ViewChild('tagInput') tagInput!: ElementRef;

  // SPECIMEN:
  specimens: { barcode: string, specimen_pid: string, preparation_type: string}[] = [];
  newBarcode: string = '';
  newSpecimenPid: string = "";
  newPrepType: string = "";
  @ViewChild('barcodeInput') barcodeInput!: ElementRef<HTMLInputElement>

  // ASSET STATUS:
  statusList = ["WORKING_COPY", "ARCHIVE", "BEING_PROCESSED", "PROCESSING_HALTED", "ISSUE_WITH_MEDIA", "ISSUE_WITH_METADATA", "FOR_DELETION"];
  status: string = "";

  // ASSET_LOCKED:
  // TODO: ASSET_LOCKED is FALSE by default.
  // TODO: Find a way to set it automatically to true if all the Assets passed from the SEARCH QUERY have asset_locked true.
  // TODO: Or raise a warning somewhere.
  assetLocked: string = "";

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
    this.submitted = true;
    if (this.newTag && this.newDescription) {
      this.tags.push({ key: this.newTag.trim(), value: this.newDescription.trim() });
      this.newTag = '';
      this.newDescription = '';
      this.submitted = false;

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

  updateAssets(){
    // Creation of the JSON Body:
    const json = this.createJson();

    console.log(json);

    this.bulkUpdateService.updateAssets(json).subscribe({
      next: (response: HttpResponse<any>) => {
        const assets: any[] = response.body;
        const assetGuid: string[] = assets.map(asset => asset.asset_guid);
        this.showSuccessSnackBar(`Assets have been updated: ${assetGuid.join(", ")}`)
          .subscribe(() => {
            window.location.reload();
          })
      },
      error: (error: HttpErrorResponse) => {
        this._snackBar.open("Cannot Bulk Update Assets: " + error.error.errorMessage, "Close");
      }
    });

    this._snackBar.open("Message Received", "Undo");
  }

  createJson(){

    const jsonObject: {[key: string]: any } = {};

    if (!isEmpty(this.tags)){
      const tagsObject: {[key : string]: string} = {};

      this.tags.forEach(pair => {
        tagsObject[pair.key] = pair.value;
      })

      jsonObject['tags'] = tagsObject;
    }

    if (!isEmpty(this.status)) jsonObject['status'] = this.status;
    if (!isEmpty(this.specimens)) jsonObject['specimens'] = this.specimens;
    if (!isEmpty(this.assetLocked)) jsonObject['asset_locked'] = this.assetLocked;
    if (!isEmpty(this.subject)) jsonObject['subject'] = this.subject;
    if (!isEmpty(this.roleList)) jsonObject['restricted_access'] = this.roleList;
    if (!isEmpty(this.funding)) jsonObject['funding'] = this.funding;
    if (!isEmpty(this.formatList)) jsonObject['file_formats'] = this.formatList;
    if (!isEmpty(this.payloadType)) jsonObject['payload_type'] = this.payloadType;
    if (!isEmpty(this.parentGuid)) jsonObject['parent_guid'] = this.parentGuid;

    // TODO: FIND A WAY TO GET THIS FROM THE LOGGED IN USER OR THE LIST OF ASSETS TO BE UPDATED
    // TODO: THIS IS MOCK-UP DATA:
    jsonObject['institution'] = "test-institution";
    jsonObject['collection'] = 'test-collection';
    jsonObject['pipeline'] = "ti-p1";
    jsonObject['workstation'] = "ti-ws-01";

    return jsonObject;

    function isEmpty(value: any): boolean {
      return (
        value === '' || (Array.isArray(value) && value !== null && Object.keys(value).length === 0)
      );
    }
  }

  showSuccessSnackBar(message: string): Observable<MatSnackBarDismiss>{
    const snackBarRef = this._snackBar.open(message, "Close")
    return snackBarRef.afterDismissed();
  }

}
