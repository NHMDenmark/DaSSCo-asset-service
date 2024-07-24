import { Component, OnInit } from '@angular/core';
import {AssetGroupService} from "../../services/asset-group.service";
import {AssetGroup} from "../../types/types";
import {filter, map, take} from "rxjs";
import {MatTableDataSource} from "@angular/material/table";
import {isNotUndefined} from "@northtech/ginnungagap";
import {animate, state, style, transition, trigger} from '@angular/animations';
import {SelectionModel} from "@angular/cdk/collections";
import {MatListOption} from "@angular/material/list";
import {CacheService} from "../../services/cache.service";
import {FormControl} from "@angular/forms";
import {AuthService} from "../../services/auth.service";
import {MatDialog} from "@angular/material/dialog";

@Component({
  selector: 'dassco-asset-groups',
  templateUrl: './asset-groups.component.html',
  styleUrls: ['./asset-groups.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})
export class AssetGroupsComponent implements OnInit {
  expandedElement: AssetGroup | undefined;
  dataSource = new MatTableDataSource<AssetGroup>();
  displayedColumns = ['select', 'group_name', 'assets_count'];
  displayedColumnsExpanded = [...this.displayedColumns, 'expand'];
  isGroupCreator = true;
  username$ = this.authService.username$.pipe(filter(isNotUndefined), take(1))
  selection = new SelectionModel<AssetGroup>(true, []);
  editing = false;
  addingDigitisers = false;
  digitiserFormControl = new FormControl<string[] | null>(null);

  cachedDigitisers$ = this.cacheService.cachedDigitisers$;

  assetGroups$
    = this.assetGroupService.assetGroups$
    .pipe(
      filter(isNotUndefined),
      map(groups => this.dataSource.data = groups)
    )

  constructor(private assetGroupService: AssetGroupService
            , private cacheService: CacheService
            , public dialog: MatDialog
            , private authService: AuthService) { }

  ngOnInit(): void {
  }

  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  toggleAllRows() {
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }

    this.selection.select(...this.dataSource.data);
  }

  editGroup() {
    this.editing = !this.editing;
    this.addingDigitisers = false;
  }

  removeAssets(assets: MatListOption[], group: AssetGroup) {
    const selectedAssets: string[] = assets.map(option => option.value);
    this.assetGroupService.updateGroupRemoveAssets(group.group_name, selectedAssets)
      .subscribe(updatedGroup => {
        console.log(updatedGroup);
        if (updatedGroup) {
          this.updateDataSourceGroup(group, updatedGroup);
        }
      });
  }

  revokeAccess(users: MatListOption[], group: AssetGroup) {
    const selectedUsers: string[] = users.map(option => option.value);
    this.assetGroupService.revokeAccess(group.group_name, selectedUsers)
      .subscribe(updatedGroup => {
        if (updatedGroup) {
          this.updateDataSourceGroup(group, updatedGroup);
        }
      });
  }

  newDigitiserAccess(group: AssetGroup) {
    const selectedUsers = this.digitiserFormControl.value;
    if (selectedUsers) {
      this.assetGroupService.grantAccess(group.group_name, selectedUsers)
        .subscribe(updatedGroup => {
          if (updatedGroup) {
            this.updateDataSourceGroup(group, updatedGroup);
          }
        });
    }
  }

  updateDataSourceGroup(prevGroup: AssetGroup, newGroup: AssetGroup) {
    const i = this.dataSource.data.findIndex(g => g == prevGroup);
    this.dataSource.data[i].assets = newGroup.assets;
    this.dataSource.data[i].hasAccess = newGroup.hasAccess;
  }

}
