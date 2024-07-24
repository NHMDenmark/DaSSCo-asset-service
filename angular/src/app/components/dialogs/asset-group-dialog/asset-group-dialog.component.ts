import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {AssetGroupService} from "../../../services/asset-group.service";
import {CacheService} from "../../../services/cache.service";
import {AssetGroup} from "../../../types/types";
import {FormControl} from "@angular/forms";

@Component({
  selector: 'dassco-asset-group-dialog',
  templateUrl: './asset-group-dialog.component.html',
  styleUrls: ['./asset-group-dialog.component.scss']
})
export class AssetGroupDialogComponent implements OnInit {
  groupName: string | undefined;
  new: boolean = false;
  nameSaved = true;
  digitiserFormControl = new FormControl<string[] | null>(null);

  assetGroups$ = this.assetGroupService.assetGroups$;
  cachedDigitisers$ = this.cacheService.cachedDigitisers$;

  constructor(
    public dialogRef: MatDialogRef<AssetGroupDialogComponent>
    , @Inject(MAT_DIALOG_DATA) public data: string
    , private cacheService: CacheService
    , private assetGroupService: AssetGroupService) { }

  ngOnInit(): void {
  }

  // existingGroup(group: string | undefined) {
    // this.group = {group: group, new: false};
    // console.log('heheheheh', this.group)
  // }

  // newGroup(event: any) {
  //   event.stopPropagation();
  //   this.nameSaved = true;
  //   // this.new = true;
  // }

  cancel() {
    this.dialogRef.close();
  }

  save() {
    let group: {group: AssetGroup, new: boolean} = {group: {group_name: this.groupName, assets: undefined, hasAccess: undefined}, new: this.new};
    if (this.new) {
      const hasAccess = this.digitiserFormControl.value;
      if (hasAccess) group.group.hasAccess = hasAccess;
    }
    console.log(group)
    this.dialogRef.close(group);
  }
}
