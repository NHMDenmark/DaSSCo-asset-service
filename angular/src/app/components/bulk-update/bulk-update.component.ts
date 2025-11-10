import {Component, inject} from '@angular/core';
import {Asset, Digitiser, Funding} from '../../types/types';
import {FormArray, FormControl, FormGroup} from '@angular/forms';
import {DIALOG_DATA, DialogRef} from '@angular/cdk/dialog';
import {
  BulkUpdateService,
  GroupedIssue,
  GroupedDigitiser,
  BulkUpdatePayload
} from 'src/app/services/bulk-update.service';
import {take} from 'rxjs';

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
  groupedDigitisers$ = this.bulkUpdateService.getGroupedDigitisers(
    this.data.assets.map((asset) => asset.asset_guid as string)
  );
  deletedIssueIds: number[] = [];
  deletedDigitiserIds: number[] = [];

  constructor() {
    if (!this.data.assets) {
      return;
    }

    this.groupedIssues$.pipe(take(1)).subscribe((issues) => {
      issues.forEach((issue) => {
        this.patchIssue(issue);
      });
    });
    this.groupedDigitisers$.pipe(take(1)).subscribe((digitisers) => {
      digitisers.forEach((digitiser) => {
        this.patchDigitiser(digitiser);
      });
    });
  }

  cancel() {
    this.dialogRef.close();
  }

  save() {
    const modifiedFields: Record<string, unknown> = {};
    for (const [key, control] of Object.entries(this.bulkUpdateForm.controls)) {
      if (control?.dirty && key !== 'issues' && key !== 'complete_digitiser_list') {
        modifiedFields[key] = (control as any).value;
      }
    }

    const payload: BulkUpdatePayload = {
      assetGuids: this.data.assets.map((a) => a.asset_guid as string),
      fields: Object.keys(modifiedFields).length > 0 ? modifiedFields : undefined,
      issues: {
        add: this.issues.value.filter((i: any) => !i.issueIds),
        update: this.issues.value
          .filter((i: any) => i.issueIds && i.issueIds.length > 0)
          .map((i: any) => ({
            issueIds: i.issueIds,
            values: {
              category: i.category,
              name: i.name,
              description: i.description,
              status: i.status,
              solved: i.solved,
              notes: i.notes
            }
          })),
        delete: this.deletedIssueIds.length > 0 ? this.deletedIssueIds : undefined
      },
      digitisers: {
        add: this.digitisers.value
          .filter((d: any) => d.isNew)
          .map((d: any) => ({
            dasscoUserId: d.dasscoUserId,
            assetGuids: d.assetGuids
          })),
        delete: this.deletedDigitiserIds.length > 0 ? this.deletedDigitiserIds : undefined
      }
    };

    this.dialogRef.close(payload);
  }

  get issues() {
    return this.bulkUpdateForm.controls.issues as FormArray;
  }

  get digitisers() {
    return this.bulkUpdateForm.controls.complete_digitiser_list as FormArray;
  }

  get legality() {
    return this.bulkUpdateForm.controls.legality as FormGroup;
  }

  bulkUpdateForm = new FormGroup({
    digitiser: new FormControl<number | null>(null), // check
    complete_digitiser_list: new FormArray<
      FormGroup<{
        dasscoUserId: FormControl<number | null>;
        username: FormControl<string | null>;
        digitiserListIds: FormControl<number[] | null>;
        assetGuids: FormControl<string[] | null>;
        count: FormControl<number | null>;
        isNew: FormControl<boolean | null>;
      }>
    >([]), // check
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
    >([]), // check
    legality: new FormGroup({
      copyright: new FormControl<string | null>(null),
      license: new FormControl<string | null>(null),
      credit: new FormControl<string | null>(null)
    }), // check
    role_restrictions: new FormControl<string[] | null>(null), //  check
    status: new FormControl<string | null>(null), // check
    funding: new FormControl<number | null>(null), // check
    asset_subject: new FormControl<string | null>(null), // check
    payload_type: new FormControl<string | null>(null) // check
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

  patchDigitiser(digitiser: Partial<GroupedDigitiser>) {
    this.digitisers.push(
      new FormGroup({
        dasscoUserId: new FormControl<number | null>(digitiser.dasscoUserId ?? null),
        username: new FormControl<string | null>(digitiser.username ?? null),
        digitiserListIds: new FormControl<number[] | null>(digitiser.digitiserListIds ?? null),
        assetGuids: new FormControl<string[] | null>(digitiser.assetGuids ?? null),
        count: new FormControl<number | null>(digitiser.count ?? null),
        isNew: new FormControl<boolean | null>(false)
      })
    );
  }

  addDigitiser(digitiser: Digitiser) {
    // Check if digitiser already exists
    const exists = this.digitisers.controls.some(
      (control) => control.get('dasscoUserId')?.value === digitiser.dasscoUserId
    );

    if (!exists) {
      this.digitisers.push(
        new FormGroup({
          dasscoUserId: new FormControl<number | null>(digitiser.dasscoUserId ?? null),
          username: new FormControl<string | null>(digitiser.username ?? null),
          digitiserListIds: new FormControl<number[] | null>(null),
          assetGuids: new FormControl<string[] | null>(this.data.assets.map((asset) => asset.asset_guid as string)),
          count: new FormControl<number | null>(0),
          isNew: new FormControl<boolean | null>(true)
        })
      );
    }
  }

  isDigitiserAdded(digitiserId: number): boolean {
    return this.digitisers.controls.some((control) => control.get('dasscoUserId')?.value === digitiserId);
  }

  deleteDigitiser(index: number) {
    const digitiserGroup = this.digitisers.at(index);
    const digitiserListIds = digitiserGroup.get('digitiserListIds')?.value;
    const isNew = digitiserGroup.get('isNew')?.value;

    // If this digitiser has existing IDs (not new), add them to the deleted list
    if (!isNew && digitiserListIds && digitiserListIds.length > 0) {
      this.deletedDigitiserIds = [...this.deletedDigitiserIds, ...digitiserListIds];
    }

    // Remove the digitiser from the form array
    this.digitisers.removeAt(index);
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
