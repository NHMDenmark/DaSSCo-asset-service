import {Component, inject} from '@angular/core';
import {FormControl} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';
import {isNotUndefined} from '@northtech/ginnungagap';
import {filter, take} from 'rxjs';
import {AssetGroupService} from '../../../services/asset-group.service';
import {AuthService} from '../../../services/auth.service';
import {KeycloakUserFrontend} from '../../../types/keycloak-user-frontend';
import {AssetGroup} from '../../../types/types';
import {KeycloakUserService} from '../../../services/keycloak-user.service';

interface AssetGroupDialogResult {
  group: AssetGroup;
  new: boolean;
}

type AssetGroupDialogMode = 'existing' | 'new';

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

  groupName: string | undefined;
  mode: AssetGroupDialogMode = 'existing';
  new = false;
  nameSaved = true;

  readonly digitiserFormControl = new FormControl<KeycloakUserFrontend[] | null>(null);

  readonly ownAssetGroups$ = this.assetGroupService.ownAssetGroups$;
  readonly username$ = this.authService.username$.pipe(filter(isNotUndefined), take(1));

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    this.dialogRef.close(this.buildDialogResult());
  }

  selectExistingGroup(): void {
    this.mode = 'existing';
    this.new = false;
  }

  selectMode(mode: AssetGroupDialogMode): void {
    this.mode = mode;
    this.new = mode === 'new';
    this.groupName = undefined;
    this.digitiserFormControl.reset(null);
  }

  markNameUnsaved(): void {
    this.nameSaved = false;
  }

  canSave(): boolean {
    return !!this.groupName?.trim();
  }

  private buildDialogResult(): AssetGroupDialogResult {
    const group: AssetGroup = {
      group_name: this.groupName,
      assets: undefined,
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
