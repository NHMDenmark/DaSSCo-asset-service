import {Component, inject} from '@angular/core';
import {Asset, Digitiser, Funding, Legality} from '../../types/types';
import {FormArray, FormControl, FormGroup} from '@angular/forms';
import {DIALOG_DATA, DialogRef} from '@angular/cdk/dialog';
import {BulkUpdateService, GroupedIssue} from 'src/app/services/bulk-update.service';

@Component({
  selector: 'dassco-bulk-update',
  templateUrl: './bulk-update.component.html',
  styleUrls: ['./bulk-update.component.scss']
})
export class BulkUpdateComponent {
  bulkUpdateService = inject(BulkUpdateService);
  dialogRef = inject(DialogRef);
  data: {assets: Asset[]} = inject(DIALOG_DATA);
  digitiserList$ = this.bulkUpdateService.getDigitiserList();
  fundingList$ = this.bulkUpdateService.getFundingList();
  subjectsList$ = this.bulkUpdateService.getSubjects();
  rolesList$ = this.bulkUpdateService.getRoles();
  issueCategoriesList$ = this.bulkUpdateService.getIssueCategories();
  statusesList$ = this.bulkUpdateService.getStatuses();
  groupedIssues$ = this.bulkUpdateService.getGroupedIssues(this.data.assets.map((asset) => asset.asset_guid as string));
  deletedIssueIds: number[] = [];

  constructor() {
    if (!this.data.assets) {
      return;
    }

    this.groupedIssues$.subscribe((issues) => {
      issues.forEach((issue) => {
        this.patchIssue(issue);
      });
    });
  }

  cancel() {
    this.dialogRef.close();
  }

  save() {
    const formValue = this.bulkUpdateForm.value;
    this.dialogRef.close({
      ...formValue,
      deletedIssueIds: this.deletedIssueIds
    });
  }

  get issues() {
    return this.bulkUpdateForm.controls.issues as FormArray;
  }

  bulkUpdateForm = new FormGroup({
    digitiser: new FormControl<number | null>(null), // check
    complete_digitiser_list: new FormControl<string[] | null>(null),
    asset_locked: new FormControl<boolean | null>(null), // check
    audited: new FormControl<boolean | null>(null), // check
    camera_setting_control: new FormControl<string | null>(null), // check
    metadata_source: new FormControl<string | null>(null), // check
    push_to_specify: new FormControl<boolean | null>(null), // check
    issues: new FormArray<
      FormGroup<{
        category: FormControl<string | null>;
        name: FormControl<string | null>;
        description: FormControl<string | null>;
        status: FormControl<string | null>;
        solved: FormControl<boolean | null>;
        notes: FormControl<string | null>;
        issueIds: FormControl<number[] | null>;
        assetGuids: FormControl<string[] | null>;
        count: FormControl<number | null>;
      }>
    >([]),
    legality: new FormControl<Legality | null>(null),
    role_restrictions: new FormControl<string[] | null>(null), //  check
    status: new FormControl<string | null>(null), // check
    funding: new FormControl<number | null>(null), // check
    asset_subject: new FormControl<string | null>(null), // check
    payload_type: new FormControl<string | null>(null)
  });

  patchIssue(issue: Partial<GroupedIssue>) {
    this.issues.push(
      new FormGroup({
        category: new FormControl<string | null>(issue.category ?? null),
        name: new FormControl<string | null>(issue.name ?? null),
        description: new FormControl<string | null>(issue.description ?? null),
        status: new FormControl<string | null>(issue.status ?? null),
        solved: new FormControl<boolean | null>(issue.solved ?? null),
        notes: new FormControl<string | null>(issue.notes ?? null),
        issueIds: new FormControl<number[] | null>(issue.issueIds ?? null),
        assetGuids: new FormControl<string[] | null>(issue.assetGuids ?? null),
        count: new FormControl<number | null>(issue.count ?? null)
      })
    );
  }

  addIssue() {
    this.issues.push(
      new FormGroup({
        category: new FormControl<string | null>(null),
        name: new FormControl<string | null>(null),
        description: new FormControl<string | null>(null),
        status: new FormControl<string | null>(null),
        solved: new FormControl<boolean | null>(false),
        notes: new FormControl<string | null>(null),
        issueIds: new FormControl<number[] | null>(null),
        assetGuids: new FormControl<string[] | null>(this.data.assets.map((asset) => asset.asset_guid as string)),
        count: new FormControl<number | null>(0)
      })
    );
  }

  deleteIssue(index: number) {
    const issueGroup = this.issues.at(index);
    const issueIds = issueGroup.get('issueIds')?.value;

    // If this issue has existing IDs, add them to the deleted list
    if (issueIds && issueIds.length > 0) {
      this.deletedIssueIds = [...this.deletedIssueIds, ...issueIds];
    }

    // Remove the issue from the form array
    this.issues.removeAt(index);
  }

  trackByDasscoUserId(_index: number, digitiser: Digitiser) {
    return digitiser.dasscoUserId;
  }
  trackBy(_index: number, status: string) {
    return status;
  }
  trackByFundingId(_index: number, funding: Funding) {
    return funding.funding_id;
  }
}
