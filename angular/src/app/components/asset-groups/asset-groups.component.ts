import {Component, OnInit, ViewChild} from '@angular/core';
import {AssetGroupService} from "../../services/asset-group.service";
import {AssetGroup, DasscoError} from "../../types/types";
import {filter, map, startWith, take} from "rxjs";
import {MatTableDataSource} from "@angular/material/table";
import {isNotUndefined} from "@northtech/ginnungagap";
import {animate, state, style, transition, trigger} from '@angular/animations';
import {SelectionModel} from "@angular/cdk/collections";
import {MatListOption} from "@angular/material/list";
import {CacheService} from "../../services/cache.service";
import {FormControl} from "@angular/forms";
import {AuthService} from "../../services/auth.service";
import {MatDialog} from "@angular/material/dialog";
import {Router} from "@angular/router";
import {MatCheckbox} from "@angular/material/checkbox";
import {combineLatest} from "rxjs";
import {
  IllegalAssetGroupDialogComponent
} from "../dialogs/illegal-asset-group-dialog/illegal-asset-group-dialog.component";

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
  @ViewChild('checkbox', {static: false}) checkboxes: MatCheckbox[] | undefined;

  expandedElement: AssetGroup | undefined;
  dataSource = new MatTableDataSource<AssetGroup>();
  displayedColumns = ['select', 'group_name', 'assets_count'];
  displayedColumnsExpanded = [...this.displayedColumns, 'expand'];
  groupSelection = new SelectionModel<AssetGroup>(true, []);
  editing = false;
  digitiserFormControl = new FormControl<string[] | null>(null);

  username$ = this.authService.username$.pipe(filter(isNotUndefined), take(1)); // to check if user is creator
  cachedDigitisers$ = this.cacheService.cachedDigitisers$;

  assetGroups$
    = combineLatest([
    this.assetGroupService.assetGroups$.pipe(startWith([]),filter(isNotUndefined)),
    this.authService.username$.pipe(filter(isNotUndefined))
  ])
    .pipe(
      map(([groups, username]) => {
        groups.forEach((group: AssetGroup) => {
          if (group.groupCreator == username) group.isCreator = true;
        })
        return this.dataSource.data = groups;
      })
    )

  constructor(private assetGroupService: AssetGroupService
            , private cacheService: CacheService
            , private router: Router
            , public dialog: MatDialog
            , private authService: AuthService) { }

  ngOnInit(): void {
  }

  isAllSelected() {
    const numSelected = this.groupSelection.selected.length;
    const numRows = this.dataSource.data.filter(group => group.isCreator).length;
    return numSelected === numRows;
  }

  toggleAllRows() {
    if (this.isAllSelected()) {
      this.groupSelection.clear();
      return;
    }

    this.groupSelection.select(...this.dataSource.data.filter(group => group.isCreator));
  }

  editGroup() {
    this.editing = !this.editing;
  }

  removeMatListOptionAssets(assets: MatListOption[], group: AssetGroup) {
    const selectedAssets: string[] = assets.map(option => option.value);
    this.removeAssets(selectedAssets, group);
  }

  removeAssets(assets: string[], group: AssetGroup) {
    // const selectedAssets: string[] = assets.map(option => option.value);
    this.assetGroupService.updateGroupRemoveAssets(group.group_name, assets)
      .subscribe(updatedGroup => {
        if (updatedGroup) {
          this.updateDataSourceGroup(group, updatedGroup, false);
        }
      });
  }

  revokeAccess(users: MatListOption[], group: AssetGroup) {
    const selectedUsers: string[] = users.map(option => option.value);
    this.assetGroupService.revokeAccess(group.group_name, selectedUsers)
      .subscribe(updatedGroup => {
        if (updatedGroup) {
          this.updateDataSourceGroup(group, updatedGroup, false);
        }
      });
  }

  newDigitiserAccess(group: AssetGroup) {
    const selectedUsers = this.digitiserFormControl.value;
    if (selectedUsers) {
      this.assetGroupService.grantAccess(group.group_name, selectedUsers)
        .subscribe(response => {
          if ((response as DasscoError).errorCode) {
            const error = response as DasscoError;
            if (error.body) {
              const errorDialogRef = this.dialog.open(IllegalAssetGroupDialogComponent, {
                width: '500px',
                data: {
                  assets: error.body,
                  removable: true
                }
              });

              errorDialogRef.afterClosed().subscribe((assets: string[] | undefined) => {
                if (assets) {
                  this.removeAssets(assets, group);
                }
              })
            }
          }
          if ((response as AssetGroup).group_name) {
            this.updateDataSourceGroup(group, (response as AssetGroup), false);
          }
        });
    }
  }

  updateDataSourceGroup(prevGroup: AssetGroup, newGroup: AssetGroup | null, remove: boolean) {
    const i = this.dataSource.data.findIndex(g => g == prevGroup);
    if (remove) {
      this.dataSource.data.splice(i, 1);
      this.dataSource._updateChangeSubscription();
    } else {
      if (newGroup) {
        this.dataSource.data[i].assets = newGroup.assets;
        this.dataSource.data[i].hasAccess = newGroup.hasAccess;
      }
    }
  }

  goToAsset(assetGuid: string) {
    this.router.navigate(['/detailed-view/' + assetGuid]);
  }

  deleteGroups() {
    if (this.groupSelection.selected) {
      this.groupSelection.selected.forEach(group => {
        this.assetGroupService.deleteGroup(group.group_name)
          .subscribe(() => this.updateDataSourceGroup(group, null, true))
      })
    }
  }

}
