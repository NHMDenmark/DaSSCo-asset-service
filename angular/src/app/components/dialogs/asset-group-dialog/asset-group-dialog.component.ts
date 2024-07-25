import {Component, OnInit} from '@angular/core';
import {MatDialogRef} from "@angular/material/dialog";
import {AssetGroupService} from "../../../services/asset-group.service";
import {CacheService} from "../../../services/cache.service";
import {AssetGroup, Digitiser} from "../../../types/types";
import {FormControl} from "@angular/forms";
import {filter, map, take} from "rxjs";
import {isNotUndefined} from "@northtech/ginnungagap";
import {AuthService} from "../../../services/auth.service";
import {combineLatest} from "rxjs";

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

  cachedDigitisers$
    = combineLatest([
      this.authService.username$.pipe(filter(isNotUndefined), take(1)),
      this.cacheService.cachedDigitisers$.pipe(filter(isNotUndefined))
  ])
    .pipe(
      map(([username, digitisers]) => {
        const digitiserMap= new Map<string, Digitiser[]>(Object.entries(digitisers));
        if (digitiserMap.has(username)) {
          digitiserMap.delete(username); // so the user can't choose themselves
        }
        return digitiserMap;
      })
    )

  constructor(
    public dialogRef: MatDialogRef<AssetGroupDialogComponent>
    , private cacheService: CacheService
    , private authService: AuthService
    , private assetGroupService: AssetGroupService) { }

  ngOnInit(): void {
  }

  cancel() {
    this.dialogRef.close();
  }

  save() {
    let group: {group: AssetGroup, new: boolean} = {group: {group_name: this.groupName, assets: undefined, hasAccess: undefined, groupCreator: undefined, isCreator: undefined}, new: this.new};
    if (this.new) {
      const hasAccess = this.digitiserFormControl.value;
      if (hasAccess) group.group.hasAccess = hasAccess;
    }
    this.dialogRef.close(group);
  }
}
