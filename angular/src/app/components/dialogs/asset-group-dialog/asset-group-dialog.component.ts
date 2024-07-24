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
    , @Inject(MAT_DIALOG_DATA) public newGroupOnly: boolean
    , private cacheService: CacheService
    , private assetGroupService: AssetGroupService) { }

  ngOnInit(): void {
  }

  cancel() {
    this.dialogRef.close();
  }

  save() {
    let group: {group: AssetGroup, new: boolean} = {group: {group_name: this.groupName, assets: undefined, hasAccess: undefined, groupCreator: undefined}, new: this.new};
    if (this.new) {
      const hasAccess = this.digitiserFormControl.value;
      if (hasAccess) group.group.hasAccess = hasAccess;
    }
    this.dialogRef.close(group);
  }
}
