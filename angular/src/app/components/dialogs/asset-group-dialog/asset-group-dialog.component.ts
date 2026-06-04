import {Component, inject} from '@angular/core';
import {FormControl} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';
import {isNotUndefined} from '@northtech/ginnungagap';
import {combineLatest, debounceTime, filter, map, startWith, take} from 'rxjs';
import {AssetGroupService} from '../../../services/asset-group.service';
import {AuthService} from '../../../services/auth.service';
import {KeycloakUserService} from '../../../services/keycloak-user.service';
import {AssetGroup} from '../../../types/types';

interface AssetGroupDialogResult {
  group: AssetGroup;
  new: boolean;
}

@Component({
  selector: 'dassco-asset-group-dialog',
  templateUrl: './asset-group-dialog.component.html',
  styleUrls: ['./asset-group-dialog.component.scss']
})
export class AssetGroupDialogComponent {
  private readonly authService = inject(AuthService);
  private readonly keycloakUserService = inject(KeycloakUserService);
  private readonly assetGroupService = inject(AssetGroupService);
  private readonly dialogRef = inject(MatDialogRef<AssetGroupDialogComponent>);

  groupName: string | undefined;
  new = false;
  nameSaved = true;

  readonly digitiserFormControl = new FormControl<string[] | null>(null);
  readonly digitiserSearch = new FormControl<string>('', {nonNullable: true});

  readonly ownAssetGroups$ = this.assetGroupService.ownAssetGroups$;
  readonly filteredKeycloakUsers$ = combineLatest([
    this.authService.username$.pipe(filter(isNotUndefined), take(1)),
    this.keycloakUserService.getFilteredKeycloakUsers(
      this.digitiserSearch.valueChanges.pipe(debounceTime(150), startWith(''))
    )
  ]).pipe(map(([username, users]) => users.filter((user) => user.username !== username)));

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    this.dialogRef.close(this.buildDialogResult());
  }

  selectExistingGroup(): void {
    this.new = false;
  }

  openNewGroupPanel(): void {
    this.groupName = undefined;
    this.new = true;
  }

  markNameUnsaved(): void {
    this.nameSaved = false;
  }

  private buildDialogResult(): AssetGroupDialogResult {
    const group: AssetGroup = {
      group_name: this.groupName,
      assets: undefined,
      hasAccess: this.getSelectedDigitisers(),
      groupCreator: undefined,
      isCreator: undefined
    };

    return {
      group,
      new: this.new
    };
  }

  private getSelectedDigitisers(): string[] {
    if (!this.new) return [];

    return this.digitiserFormControl.value ?? [];
  }
}
