import {Component, ElementRef, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {BulkUpdateService} from "../../services/bulk-update.service";
import {MatSnackBar, MatSnackBarDismiss} from "@angular/material/snack-bar";
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {Observable} from "rxjs";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";


@Component({
  selector: 'dassco-bulk-update',
  templateUrl: './bulk-update.component.html',
  styleUrls: ['./bulk-update.component.scss']
})
export class BulkUpdateComponent implements OnInit {

  @ViewChild('confirmationDialog') confirmationDialog : TemplateRef<any> = {} as TemplateRef<any>;
  private dialogRef!: MatDialogRef<any>;

  constructor(private bulkUpdateService: BulkUpdateService,
              private _snackBar: MatSnackBar,
              private dialog : MatDialog) { }

  ngOnInit(): void {
  }

  // TAGS:
  tags: { key: string, value: string }[] = [];
  newTag: string = '';
  newDescription: string = '';
  submitted: boolean = false;
  @ViewChild('tagInput') tagInput!: ElementRef;

  // ASSET STATUS:
  statusList = ["WORKING_COPY", "ARCHIVE", "BEING_PROCESSED", "PROCESSING_HALTED", "ISSUE_WITH_MEDIA", "ISSUE_WITH_METADATA", "FOR_DELETION"];
  status: string = "";

  // Asset List (TODO: Get from the Frontend)
  assetList : string[] = ["test-bulk-update-1", "test-bulk-update-2"];

  // ASSET_LOCKED:
  assetLocked: string = "";

  // SUBJECT:
  subject: string = "";

  // FUNDING:
  funding: string = "";

  // PAYLOAD_TYPE
  payloadType: string = "";

  // PARENT_GUID
  parentGuid: string = "";

  // DIGITISER
  digitiser: string = "";

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

  remove(pair: { key: string, value: string }): void {
    const index = this.tags.indexOf(pair);
    if (index >= 0) {
      this.tags.splice(index, 1);
    }
  }

  updateAssets(){

    // Creation of the JSON Body:
    const json = this.createJson();

    // Creation of the url:
    const assets : string = this.assetList.map(item => `assets=${item}`).join('&');

    this.bulkUpdateService.updateAssets(json, assets).subscribe({
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
    if (!isEmpty(this.assetLocked)) jsonObject['asset_locked'] = this.assetLocked;
    if (!isEmpty(this.subject)) jsonObject['subject'] = this.subject;
    if (!isEmpty(this.funding)) jsonObject['funding'] = this.funding;
    if (!isEmpty(this.payloadType)) jsonObject['payload_type'] = this.payloadType;
    if (!isEmpty(this.parentGuid)) jsonObject['parent_guid'] = this.parentGuid;
    if (!isEmpty(this.digitiser)) jsonObject['digitiser'] = this.digitiser;

    // TODO: FIND A WAY TO GET THIS FROM THE LOGGED IN USER OR THE LIST OF ASSETS TO BE UPDATED
    // TODO: THIS IS MOCK-UP DATA:
    jsonObject['pipeline'] = "i2_p1";
    jsonObject['workstation'] = "i2_w1";

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

  openConfirmationDialog() {
    this.dialogRef = this.dialog.open(this.confirmationDialog, {
      data: { assets: this.assetList }
    });

    this.dialogRef.afterClosed().subscribe(result => {
      if (result === 'proceed') {
        this.updateAssets();
      }
    });
  }

  onCancel(): void {
    this.dialog.closeAll();
  }

  onDialogProceed(){
    this.dialogRef.close('proceed');
  }

}
