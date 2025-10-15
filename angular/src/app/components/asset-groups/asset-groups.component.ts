import {Component} from '@angular/core';
import {AssetGroupService} from '../../services/asset-group.service';
import {AssetGroup, DasscoError} from '../../types/types';
import {filter, map, startWith, take} from 'rxjs';
import {MatTableDataSource} from '@angular/material/table';
import {isNotUndefined} from '@northtech/ginnungagap';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {SelectionModel} from '@angular/cdk/collections';
import {MatListOption} from '@angular/material/list';
import {CacheService} from '../../services/cache.service';
import {FormControl} from '@angular/forms';
import {AuthService} from '../../services/auth.service';
import {MatDialog} from '@angular/material/dialog';
import {Router} from '@angular/router';
import {combineLatest} from 'rxjs';
import {MatSnackBar} from '@angular/material/snack-bar';
import {IllegalAssetGroupDialogComponent} from '../dialogs/illegal-asset-group-dialog/illegal-asset-group-dialog.component';
import {DetailedViewService} from '../../services/detailed-view.service';

@Component({
  selector: 'dassco-asset-groups',
  templateUrl: './asset-groups.component.html',
  styleUrls: ['./asset-groups.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)'))
    ])
  ]
})
export class AssetGroupsComponent {
  expandedElement: AssetGroup | undefined;
  dataSource = new MatTableDataSource<AssetGroup>();
  displayedColumns = ['select', 'group_name', 'assets_count'];
  displayedColumnsExpanded = [...this.displayedColumns, 'expand'];
  groupSelection = new SelectionModel<AssetGroup>(true, []);
  editing = false;
  downloadingCompleteAssets = false;
  digitiserFormControl = new FormControl<string[] | null>(null);

  username$ = this.authService.username$.pipe(filter(isNotUndefined), take(1)); // to check if user is creator
  cachedDigitisers$ = this.cacheService.cachedDigitisers$;

  assetGroups$ = combineLatest([
    this.assetGroupService.assetGroups$.pipe(startWith([]), filter(isNotUndefined)),
    this.authService.username$.pipe(filter(isNotUndefined))
  ]).pipe(
    map(([groups, username]) => {
      groups.forEach((group: AssetGroup) => {
        if (group.groupCreator == username) group.isCreator = true;
      });
      return (this.dataSource.data = groups);
    })
  );

  constructor(
    private assetGroupService: AssetGroupService,
    private cacheService: CacheService,
    private router: Router,
    public dialog: MatDialog,
    private _snackBar: MatSnackBar,
    private authService: AuthService,
    private detailedViewService: DetailedViewService
  ) {}

  isAllSelected() {
    const numSelected = this.groupSelection.selected.length;
    const numRows = this.dataSource.data.filter((group) => group.isCreator).length;
    return numSelected === numRows;
  }

  toggleAllRows() {
    if (this.isAllSelected()) {
      this.groupSelection.clear();
      return;
    }

    this.groupSelection.select(...this.dataSource.data.filter((group) => group.isCreator));
  }

  editGroup() {
    this.editing = !this.editing;
  }

  downloadCompleteAssets() {
    this.downloadingCompleteAssets = !this.downloadingCompleteAssets;
  }

  removeMatListOptionAssets(assets: MatListOption[], group: AssetGroup) {
    const selectedAssets: string[] = assets.map((option) => option.value);
    this.removeAssets(selectedAssets, group);
  }

  removeAssets(assets: string[], group: AssetGroup) {
    // const selectedAssets: string[] = assets.map(option => option.value);
    this.assetGroupService.updateGroupRemoveAssets(group.group_name, assets).subscribe((updatedGroup) => {
      if (updatedGroup) {
        this.updateDataSourceGroup(group, updatedGroup, false);
      }
    });
  }

  downloadCSV(assets: MatListOption[]) {
    const selectedAssets: string[] = assets.map((option) => option.value);
    this.detailedViewService.postCsv(selectedAssets).subscribe({
      next: (response) => {
        let guid: string = response.body;
        if (response.status == 200) {
          this.detailedViewService.getFile(guid, 'assets.csv').subscribe({
            next: (data) => {
              const url = window.URL.createObjectURL(data);
              const link = document.createElement('a');
              link.href = url;
              link.download = 'assets.csv';

              document.body.appendChild(link);
              link.click();

              document.body.removeChild(link);
              window.URL.revokeObjectURL(url);

              this.detailedViewService.deleteFile(guid).subscribe({
                next: () => {},
                error: () => {
                  this.openSnackBar(
                    "There's been an error deleting the CSV file",
                    "There's been an error deleting the CSV file"
                  );
                }
              });
            },
            error: () => {
              this.openSnackBar(
                'There has been an error downloading the CSV file.',
                'There has been an error downloading the CSV file.'
              );
            }
          });
        }
      },
      error: (error) => {
        this.openSnackBar(error.error, 'Error downloading CSV');
      }
    });
  }

  downloadZip(assets: MatListOption[]) {
    const selectedAssets: string[] = assets.map((option) => option.value);
    this.detailedViewService.postCsv(selectedAssets).subscribe({
      next: (response) => {
        if (response.status == 200) {
          let guid: string = response.body;
          this.detailedViewService.postZip(guid, selectedAssets).subscribe({
            next: (response) => {
              if (response.status == 200) {
                this.detailedViewService.getFile(guid, 'assets.zip').subscribe({
                  next: (data) => {
                    const url = window.URL.createObjectURL(data);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = 'assets.zip';

                    document.body.appendChild(link);
                    link.click();

                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);

                    this.detailedViewService.deleteFile(guid).subscribe({
                      next: () => {},
                      error: () => {
                        this.openSnackBar('There has been an error deleting the files', 'Close');
                      }
                    });
                  },
                  error: () => {
                    this.openSnackBar('There has been an error downloading the ZIP file.', 'Close');
                  }
                });
              }
            },
            error: () => {
              this.openSnackBar('There has been an error saving the files to the Temp Folder', 'Close');
            }
          });
        }
      },
      error: () => {
        this.openSnackBar('There has been an error saving the CSV File', 'Close');
      }
    });
  }

  revokeAccess(users: MatListOption[], group: AssetGroup) {
    const selectedUsers: string[] = users.map((option) => option.value);
    this.assetGroupService.revokeAccess(group.group_name, selectedUsers).subscribe((updatedGroup) => {
      if (updatedGroup) {
        this.updateDataSourceGroup(group, updatedGroup, false);
      }
    });
  }

  newDigitiserAccess(group: AssetGroup) {
    const selectedUsers = this.digitiserFormControl.value;
    if (selectedUsers) {
      this.assetGroupService.grantAccess(group.group_name, selectedUsers).subscribe((response) => {
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
            });
          }
        }
        if ((response as AssetGroup).group_name) {
          this.updateDataSourceGroup(group, response as AssetGroup, false);
        }
      });
    }
  }

  updateDataSourceGroup(prevGroup: AssetGroup, newGroup: AssetGroup | null, remove: boolean) {
    const i = this.dataSource.data.findIndex((g) => g == prevGroup);
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
      const groupNames = this.groupSelection.selected.map((gs) => gs.group_name).filter((g) => isNotUndefined(g));
      if (groupNames.length === 0) return;
      this.assetGroupService
        .deleteGroups(groupNames as string[])
        .pipe(take(1))
        .subscribe({
          next: (success) => {
            this.groupSelection.clear();
            this.dataSource.data = [...this.dataSource.data.filter((ag) => !groupNames.includes(ag.group_name ?? ''))];
            this.openSnackBar(success, `${groupNames.length} group(s) deleted`);
          },
          error: () => this.openSnackBar(undefined, "Couldn't delete groups")
        });
    }
  }

  openSnackBar(object: any | undefined, success: string) {
    if (object) {
      this._snackBar.open(success, 'OK', {duration: 5000});
    } else {
      this._snackBar.open('An error occurred. Try again.', 'OK', {duration: 5000});
    }
  }
}
