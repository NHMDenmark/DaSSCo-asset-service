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
  selectedRole: string = "";

  @ViewChild('tagInput') tagInput!: ElementRef<HTMLInputElement>

  add(): void {
    if (this.newTag.trim()) {
      this.tags.push({ key: this.newTag.trim(), value: this.newDescription.trim() });
      this.newTag = '';
      this.newDescription = '';

      this.tagInput.nativeElement.focus();
    }
  }

  addRole(): void {
    this.roleList.push(this.selectedRole);
  }

  remove(pair: { key: string, value: string }): void {
    const index = this.tags.indexOf(pair);
    if (index >= 0) {
      this.tags.splice(index, 1);
    }
  }

  removeRole(role: string): void {
    const index = this.roleList.indexOf(role);
    if (index >= 0) {
      this.tags.splice(index, 1);
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
      asset_locked: this.assetLocked,
      subject: this.subject,
      restricted_asset: this.roleList
    }, null, 2)
    console.log(json);
  }

}
