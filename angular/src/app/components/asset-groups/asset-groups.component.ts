import {Component, inject} from '@angular/core';
import {AssetGroupService} from '../../services/asset-group.service';
import {AssetGroup, DasscoError} from '../../types/types';
import {combineLatest, filter, map, startWith, switchMap, take} from 'rxjs';
import {MatTableDataSource} from '@angular/material/table';
import {isNotUndefined} from '@northtech/ginnungagap';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {SelectionModel} from '@angular/cdk/collections';
import {MatListOption} from '@angular/material/list';
import {FormControl} from '@angular/forms';
import {AuthService} from '../../services/auth.service';
import {MatDialog} from '@angular/material/dialog';
import {Router} from '@angular/router';
import {MatSnackBar} from '@angular/material/snack-bar';
import {IllegalAssetGroupDialogComponent} from '../dialogs/illegal-asset-group-dialog/illegal-asset-group-dialog.component';
import {DetailedViewService} from '../../services/detailed-view.service';
import {QueryToOtherPages} from '../../services/query-to-other-pages.service';
import {KeycloakUserFrontend} from '../../types/keycloak-user-frontend';
import {AssetBundleDownloadService} from '../../services/asset-bundle-download.service';
import {AssetGroupDialogComponent} from '../dialogs/asset-group-dialog/asset-group-dialog.component';

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
  private readonly queryToOtherPagesService = inject(QueryToOtherPages);
  private readonly assetGroupService = inject(AssetGroupService);
  private readonly router = inject(Router);
  readonly dialog = inject(MatDialog);
  private _snackBar = inject(MatSnackBar);
  private readonly authService = inject(AuthService);
  private readonly detailedViewService = inject(DetailedViewService);
  private readonly assetBundleDownloadService = inject(AssetBundleDownloadService);
  expandedElement: AssetGroup | undefined;
  dataSource = new MatTableDataSource<AssetGroup>();
  displayedColumns = ['select', 'group_name', 'assets_count'];
  displayedColumnsExpanded = [...this.displayedColumns, 'row_actions'];
  groupSelection = new SelectionModel<AssetGroup>(true, []);
  editing = false;
  downloadingCompleteAssets = false;
  auditing = false;
  digitiserFormControl = new FormControl<KeycloakUserFrontend[] | null>(null);
  username$ = this.authService.username$.pipe(filter(isNotUndefined), take(1)); // to check if user is creator

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

  expandElement(element: AssetGroup) {
    if (this.expandedElement !== element) {
      this.expandedElement = element;
      this.editing = false;
      this.downloadingCompleteAssets = false;
      this.auditing = false;
    }
  }

  toggleRowExpansion(element: AssetGroup) {
    this.expandedElement = this.expandedElement === element ? undefined : element;
    this.editing = false;
    this.downloadingCompleteAssets = false;
    this.auditing = false;
  }

  editGroup() {
    this.editing = !this.editing;
  }

  downloadCompleteAssets() {
    this.downloadingCompleteAssets = !this.downloadingCompleteAssets;
  }

  toggleAuditing() {
    this.auditing = !this.auditing;
  }

  auditSelectedAssets(assets: MatListOption[]) {
    const selectedAssets: string[] = assets.map((option) => option.value);
    if (selectedAssets.length === 0) return;

    this.username$
      .pipe(switchMap((username) => this.assetGroupService.bulkAuditAssets(selectedAssets, username)))
      .subscribe({
        next: (results) => {
          if (this.getSnackBarErrorMessage(results)) {
            this.openSnackBar(results, 'Error auditing assets');
            return;
          }

          if (results) {
            const successCount = Object.values(results).filter((v) => v === 'Success').length;
            const failCount = selectedAssets.length - successCount;
            if (failCount > 0) {
              const errors = Object.entries(results)
                .filter(([_, v]) => v !== 'Success')
                .map(([k, v]) => `${k}: ${v}`);
              console.warn('Some assets failed to audit: \n', errors.join('\n'));
              this.openSnackBar(
                results,
                `${successCount} asset${
                  successCount > 1 ? 's' : ''
                } audited, ${failCount} failed. \n \n Check the console for more details`
              );
            } else {
              this.openSnackBar(results, `${successCount} asset(s) audited successfully`);
            }
          }
        },
        error: (error) => {
          this.openSnackBar(undefined, 'Error auditing assets');
          console.error('Bulk audit error:', error);
        }
      });
  }

  removeMatListOptionAssets(assets: MatListOption[], group: AssetGroup) {
    const selectedAssets: string[] = assets.map((option) => option.value);
    this.removeAssets(selectedAssets, group);
  }

  removeAssets(assets: string[], group: AssetGroup) {
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
        const guid: string = response.body;
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
    this.startZipDownload(selectedAssets);
  }

  downloadGroupZip(group: AssetGroup) {
    this.startZipDownload(group.assets ?? []);
  }

  isZipDownloadPreparing(assets: MatListOption[]) {
    const selectedAssets: string[] = assets.map((option) => option.value);
    return this.assetBundleDownloadService.isBundleInProgress(selectedAssets, 'internal');
  }

  isGroupZipDownloadPreparing(group: AssetGroup) {
    return this.assetBundleDownloadService.isBundleInProgress(group.assets ?? [], 'internal');
  }

  private startZipDownload(assetGuids: string[]) {
    this.assetBundleDownloadService.startBundleDownload(assetGuids, {access: 'internal'});
  }

  revokeAccess(users: MatListOption[], group: AssetGroup) {
    const selectedUsers: string[] = users.map((option) => option.value);
    console.log(selectedUsers);
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
        this.digitiserFormControl.reset(null);
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
    } else if (newGroup) {
      this.dataSource.data[i].assets = newGroup.assets;
      this.dataSource.data[i].hasAccess = newGroup.hasAccess;
    }
  }

  goToAsset(assetGuid: string) {
    const currentExpandedGroup = this.expandedElement;
    if (currentExpandedGroup?.assets) {
      this.queryToOtherPagesService.setAssets(currentExpandedGroup?.assets ?? []);
    }
    this.router.navigate(['/detailed-view/' + assetGuid]);
  }

  createGroup() {
    const dialogRef = this.dialog.open(AssetGroupDialogComponent, {
      width: '500px',
      data: {
        action: 'create-group'
      }
    });

    dialogRef
      .afterClosed()
      .pipe(
        filter(isNotUndefined),
        switchMap((group: {group: AssetGroup; new: boolean}) => this.assetGroupService.newGroup(group.group)),
        take(1)
      )
      .subscribe({
        next: (response) => {
          if ((response as AssetGroup | undefined)?.group_name) {
            const createdGroup = response as AssetGroup;
            createdGroup.isCreator = true;
            this.dataSource.data = [...this.dataSource.data, createdGroup];
            this.openSnackBar(response, `The group "${createdGroup.group_name}" has been created.`);
            return;
          }

          this.openSnackBar(response, "Couldn't create group");
        },
        error: (error) => this.openSnackBar(error, "Couldn't create group")
      });
  }

  deleteGroups() {
    if (this.groupSelection.selected) {
      const groupsToDelete = this.groupSelection.selected
        .map((gs) => gs.group_id)
        .filter((id): id is number => id !== undefined);
      if (groupsToDelete.length === 0) return;

      const deletedGroupNames = this.groupSelection.selected
        .map((gs) => gs.group_name)
        .filter(isNotUndefined);

      this.assetGroupService
        .deleteGroups(groupsToDelete)
        .pipe(take(1))
        .subscribe({
          next: (response) => {
            if (this.getSnackBarErrorMessage(response)) {
              this.openSnackBar(response, "Couldn't delete groups");
              return;
            }

            this.groupSelection.clear();
            this.dataSource.data = [
              ...this.dataSource.data.filter((ag) => !groupsToDelete.includes(ag.group_id as number))
            ];
            this.openSnackBar(response, `${deletedGroupNames.length} group(s) deleted`);
          },
          error: (error) => this.openSnackBar(error, "Couldn't delete groups")
        });
    }
  }

  private getSnackBarErrorMessage(error: unknown): string | undefined {
    if (!error) return undefined;
    if (typeof error === 'string') return error;

    const candidate = error as {
      errorMessage?: string;
      body?: string;
      message?: string;
      error?: any;
    };

    if (candidate.errorMessage) return candidate.errorMessage;
    if (candidate.body) return candidate.body;
    if (candidate.message) return candidate.message;

    if (candidate.error) {
      if (typeof candidate.error === 'string') return candidate.error;

      const nested = candidate.error as {errorMessage?: string; body?: string; message?: string};
      if (nested.errorMessage) return nested.errorMessage;
      if (nested.body) return nested.body;
      if (nested.message) return nested.message;
    }

    return undefined;
  }

  openSnackBar(object: any | undefined, success: string) {
    const errorMessage = this.getSnackBarErrorMessage(object);

    if (errorMessage) {
      this._snackBar.open(errorMessage, 'OK', {duration: 3000});
    } else if (object) {
      this._snackBar.open(success, 'OK', {duration: 3000});
    } else {
      this._snackBar.open('An error occurred. Try again.', 'OK', {duration: 3000});
    }
  }
}
