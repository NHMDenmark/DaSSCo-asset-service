import {ConnectedPosition} from '@angular/cdk/overlay';
import {Component, ElementRef, Input, ViewChild} from '@angular/core';
import {FormControl} from '@angular/forms';
import {BehaviorSubject, startWith} from 'rxjs';
import {KeycloakUserService} from '../../services/keycloak-user.service';
import {KeycloakUserFrontend} from '../../types/keycloak-user-frontend';

@Component({
  selector: 'dassco-digitiser-list',
  templateUrl: './digitiser-list.component.html',
  styleUrls: ['./digitiser-list.component.scss']
})
export class DigitiserListComponent {
  @ViewChild('searchInput') private searchInput?: ElementRef<HTMLInputElement>;
  @Input() selectedUsersControl = new FormControl<KeycloakUserFrontend[] | null>(null);
  @Input() label = 'Select digitisers';
  @Input() multiple = true;
  @Input() excludedUsernames: string[] = [];
  @Input() optionsId = 'digitiser-options';
  @Input() emptyText = 'No available digitisers found';

  private readonly overlayOpenSubject = new BehaviorSubject(false);
  readonly overlayOpen$ = this.overlayOpenSubject.asObservable();
  private readonly activeDigitiserIndexSubject = new BehaviorSubject(-1);
  readonly activeDigitiserIndex$ = this.activeDigitiserIndexSubject.asObservable();
  readonly search = new FormControl<string>('', {nonNullable: true});
  readonly keycloakUsers$ = this.search.valueChanges.pipe(startWith(''));
  readonly filteredKeycloakUsers$ = this.keycloakUserService.getFilteredKeycloakUsers(this.keycloakUsers$);
  readonly loading$ = this.keycloakUserService.loading$;
  readonly overlayPositions: ConnectedPosition[] = [
    {
      originX: 'start',
      originY: 'bottom',
      overlayX: 'start',
      overlayY: 'top',
      offsetY: -1
    },
    {
      originX: 'start',
      originY: 'top',
      overlayX: 'start',
      overlayY: 'bottom',
      offsetY: 1
    }
  ];

  constructor(private readonly keycloakUserService: KeycloakUserService) {}

  openOverlay() {
    this.overlayOpenSubject.next(true);
  }

  focusSearchInput() {
    setTimeout(() => this.searchInput?.nativeElement.focus());
  }

  selectedValueText() {
    const selectedUsers = this.selectedUsersControl.value ?? [];

    if (selectedUsers.length === 0) return this.label;
    if (selectedUsers.length === 1) return selectedUsers[0].username;

    return `${selectedUsers.length} digitisers selected`;
  }

  closeOverlay() {
    this.overlayOpenSubject.next(false);
    this.activeDigitiserIndexSubject.next(-1);
  }

  clearSearch() {
    this.search.setValue('');
    this.openOverlay();
  }

  isDigitiserSelected(user: KeycloakUserFrontend) {
    return !!this.selectedUsersControl.value?.some((selectedUser) => selectedUser.username === user.username);
  }

  toggleDigitiser(user: KeycloakUserFrontend) {
    const selectedUsers = this.selectedUsersControl.value ?? [];
    if (this.isDigitiserSelected(user)) {
      this.selectedUsersControl.setValue(
        selectedUsers.filter((selectedUser) => selectedUser.username !== user.username)
      );
      this.selectedUsersControl.markAsDirty();
      return;
    }

    this.selectedUsersControl.setValue(this.multiple ? [...selectedUsers, user] : [user]);
    this.selectedUsersControl.markAsDirty();
    if (!this.multiple) this.closeOverlay();
  }

  availableDigitisers(users: KeycloakUserFrontend[]) {
    return users.filter((user) => !this.excludedUsernames.includes(user.username));
  }

  onComboboxKeydown(event: KeyboardEvent, users: KeycloakUserFrontend[]) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.openOverlay();
      this.activeDigitiserIndexSubject.next(this.getNextSelectableDigitiserIndex(users, 1));
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.openOverlay();
      this.activeDigitiserIndexSubject.next(this.getNextSelectableDigitiserIndex(users, -1));
    }

    if (event.key === 'Enter' && this.overlayOpenSubject.value) {
      event.preventDefault();
      const user = users[this.activeDigitiserIndexSubject.value];
      if (user) this.toggleDigitiser(user);
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      this.closeOverlay();
    }
  }

  setActiveDigitiserIndex(index: number) {
    this.activeDigitiserIndexSubject.next(index);
  }

  private getNextSelectableDigitiserIndex(users: KeycloakUserFrontend[], direction: 1 | -1) {
    if (users.length === 0) return 0;

    let nextIndex =
      this.activeDigitiserIndexSubject.value < 0 ? (direction === 1 ? -1 : 0) : this.activeDigitiserIndexSubject.value;
    for (let i = 0; i < users.length; i++) {
      nextIndex = (nextIndex + direction + users.length) % users.length;
      return nextIndex;
    }

    return this.activeDigitiserIndexSubject.value;
  }
}
