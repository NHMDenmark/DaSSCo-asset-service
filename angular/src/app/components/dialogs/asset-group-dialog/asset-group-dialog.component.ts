import {Component, inject} from '@angular/core';
import {FormControl} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {isNotUndefined} from '@northtech/ginnungagap';
import {filter, take} from 'rxjs';
import {AssetGroupService} from '../../../services/asset-group.service';
import {AuthService} from '../../../services/auth.service';
import {KeycloakUserFrontend} from '../../../types/keycloak-user-frontend';
import {AssetGroup} from '../../../types/types';
import {KeycloakUserService} from '../../../services/keycloak-user.service';

type AssetGroupDialogAction = 'add-assets' | 'create-group';
type AssetGroupDialogMode = 'existing' | 'new';
type AssetGroupDialogGroup = AssetGroup & {group_id?: number};

interface AssetGroupDialogResult {
  group: AssetGroupDialogGroup;
  new: boolean;
}

interface AssetGroupDialogData {
  action?: AssetGroupDialogAction;
}

@Component({
  selector: 'dassco-asset-group-dialog',
  templateUrl: './asset-group-dialog.component.html',
  styleUrls: ['./asset-group-dialog.component.scss']
})
export class AssetGroupDialogComponent {
  private readonly authService = inject(AuthService);
  // Keep injected to load users early.
  _keycloakUserService = inject(KeycloakUserService);
  private readonly assetGroupService = inject(AssetGroupService);
  private readonly dialogRef = inject(MatDialogRef<AssetGroupDialogComponent>);
  private readonly data = inject<AssetGroupDialogData | null>(MAT_DIALOG_DATA, {optional: true});
  private readonly action: AssetGroupDialogAction = this.data?.action ?? 'add-assets';

  groupName: string | undefined;
  selectedExistingGroup: AssetGroupDialogGroup | undefined;
  mode: AssetGroupDialogMode = this.isCreateGroupAction() ? 'new' : 'existing';
  new = this.isCreateGroupAction();

  readonly digitiserFormControl = new FormControl<KeycloakUserFrontend[] | null>(null);

  readonly ownAssetGroups$ = this.assetGroupService.ownAssetGroups$;
  readonly username$ = this.authService.username$.pipe(filter(isNotUndefined), take(1));

  isCreateGroupAction(): boolean {
    return this.action === 'create-group';
  }

  dialogTitle(): string {
    return this.isCreateGroupAction() ? 'New group' : 'Add assets to group';
  }

  dialogDescription(): string {
    return this.isCreateGroupAction()
      ? 'Create a new group and optionally grant access to other digitisers.'
      : 'Choose a saved group or create a new group for the selected assets.';
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    this.dialogRef.close(this.buildDialogResult());
  }

  selectExistingGroup(group: AssetGroupDialogGroup): void {
    if (this.isCreateGroupAction()) return;

    this.selectedExistingGroup = group;
    this.mode = 'existing';
    this.new = false;
  }

  selectMode(mode: AssetGroupDialogMode): void {
    if (this.isCreateGroupAction()) return;

    this.mode = mode;
    this.new = mode === 'new';
    this.groupName = undefined;
    this.selectedExistingGroup = undefined;
    this.digitiserFormControl.reset(null);
  }

  canSave(): boolean {
    if (!this.new) return this.selectedExistingGroup?.group_id !== undefined;

    return !!this.groupName?.trim();
  }

  private buildDialogResult(): AssetGroupDialogResult {
    const group: AssetGroupDialogGroup = {
      group_id: this.new ? undefined : this.selectedExistingGroup?.group_id,
      group_name: this.new ? this.groupName : this.selectedExistingGroup?.group_name,
      assets: this.isCreateGroupAction() ? [] : undefined,
      hasAccess: this.getSelectedDigitisers(),
      keycloakUsers: this.getSelectedKeycloakUsers(),
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

    return this.digitiserFormControl.value?.map((digitiser) => digitiser.username) ?? [];
  }

  private getSelectedKeycloakUsers(): KeycloakUserFrontend[] {
    if (!this.new) return [];

    return this.digitiserFormControl.value ?? [];
  }
}
