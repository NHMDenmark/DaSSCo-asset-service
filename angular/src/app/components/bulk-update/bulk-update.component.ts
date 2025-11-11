import {Component, inject} from '@angular/core';
import {Asset, Digitiser, Funding} from '../../types/types';
import {FormArray, FormControl, FormGroup} from '@angular/forms';
import {DIALOG_DATA, DialogRef} from '@angular/cdk/dialog';
import {
  BulkUpdateService,
  GroupedIssue,
  GroupedDigitiser,
  BulkUpdatePayload,
  IssuePatchBlock,
  DigitiserPatchBlock
} from 'src/app/services/bulk-update.service';
import {take} from 'rxjs';

interface IssueFormValue {
  category: string | null;
  name: string | null;
  description: string | null;
  status: string | null;
  solved: boolean | null;
  notes: string | null;
  issueIds: number[] | null;
  assetGuids: string[] | null;
  count: number | null;
}

interface DigitiserFormValue {
  dasscoUserId: number | null;
  username: string | null;
  digitiserListIds: number[] | null;
  assetGuids: string[] | null;
  count: number | null;
  isNew: boolean | null;
}

type IssueFormGroup = FormGroup<{
  category: FormControl<string | null>;
  name: FormControl<string | null>;
  description: FormControl<string | null>;
  status: FormControl<string | null>;
  solved: FormControl<boolean | null>;
  notes: FormControl<string | null>;
  issueIds: FormControl<number[] | null>;
  assetGuids: FormControl<string[] | null>;
  count: FormControl<number | null>;
}>;

interface IssueUpdateAction {
  issueIds: number[];
  action: 'update';
  values: Record<string, unknown>;
}

interface DigitiserAddAction {
  dasscoUserId: number;
  assetGuids: string[];
}

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
    const fundingValue = this.bulkUpdateForm.controls.funding.value;
    const payload: BulkUpdatePayload = {
      assetGuids: this.data.assets.map((a) => a.asset_guid as string),
      fields: this.fields.dirty ? this.filterNullValues(this.fields.value) : undefined,
      funding:
        fundingValue && fundingValue.length > 0 && this.bulkUpdateForm.controls.funding.dirty
          ? fundingValue
          : undefined,
      issues: this.issues.dirty || this.deletedIssueIds.length > 0 ? this.buildIssuesBlock() : undefined,
      digitisers:
        this.digitisers.dirty || this.deletedDigitiserIds.length > 0 ? this.buildDigitisersBlock() : undefined,
      legality: this.legality.dirty ? this.filterNullValues(this.legality.value) : undefined
    };

    // Remove undefined properties
    Object.keys(payload).forEach((key) => {
      if (payload[key as keyof BulkUpdatePayload] === undefined) {
        delete payload[key as keyof BulkUpdatePayload];
      }
    });

    this.bulkUpdateService
      .bulkUpdate(payload)
      .pipe(take(1))
      .subscribe((result) => {
        console.log(result);
        this.dialogRef.close();
      });
  }

  private filterNullValues<T extends Record<string, unknown>>(obj: T): Partial<T> {
    const filtered: Partial<T> = {};
    (Object.keys(obj) as Array<keyof T>).forEach((key) => {
      const value = obj[key];
      if (value !== null && value !== undefined) {
        filtered[key] = value;
      }
    });
    return filtered;
  }

  private buildIssuesBlock(): IssuePatchBlock | undefined {
    // Only include new issues (those without issueIds)
    const add = this.issues.controls
      .map((control, index) => ({
        control: control as IssueFormGroup,
        index,
        value: this.issues.value[index] as IssueFormValue
      }))
      .filter(({value}) => !value.issueIds || value.issueIds.length === 0)
      .map(({value}) => ({
        category: value.category ?? undefined,
        name: value.name ?? undefined,
        description: value.description ?? undefined,
        status: value.status ?? undefined,
        solved: value.solved ?? undefined,
        notes: value.notes ?? undefined
      }))
      .filter(
        (issue) =>
          issue.category !== undefined ||
          issue.name !== undefined ||
          issue.description !== undefined ||
          issue.status !== undefined ||
          issue.solved !== undefined ||
          issue.notes !== undefined
      )
      .map((issue) => ({
        category: issue.category ?? '',
        name: issue.name ?? '',
        description: issue.description ?? '',
        status: issue.status ?? '',
        solved: issue.solved ?? false,
        notes: issue.notes ?? ''
      }));

    // Only include issues that have been modified (dirty) and have issueIds
    const update = this.issues.controls
      .map((control, index) => ({
        control: control as IssueFormGroup,
        index,
        value: this.issues.value[index] as IssueFormValue
      }))
      .filter(({control, value}) => control.dirty && value.issueIds && value.issueIds.length > 0)
      .map(({value}) => {
        const issueIds = value.issueIds;
        if (!issueIds || issueIds.length === 0) {
          return null;
        }
        return {
          issueIds,
          action: 'update' as const,
          values: {
            ...(value.category !== null && value.category !== undefined && {category: value.category}),
            ...(value.name !== null && value.name !== undefined && {name: value.name}),
            ...(value.description !== null && value.description !== undefined && {description: value.description}),
            ...(value.status !== null && value.status !== undefined && {status: value.status}),
            ...(value.solved !== null && value.solved !== undefined && {solved: value.solved}),
            ...(value.notes !== null && value.notes !== undefined && {notes: value.notes})
          }
        };
      })
      .filter((action): action is IssueUpdateAction => action !== null && Object.keys(action.values).length > 0);

    const deleteIds = this.deletedIssueIds.length > 0 ? this.deletedIssueIds : undefined;

    if (add.length === 0 && update.length === 0 && !deleteIds) {
      return undefined;
    }

    return {
      ...(add.length > 0 && {add}),
      ...(update.length > 0 && {update}),
      ...(deleteIds && {delete: deleteIds})
    };
  }

  private buildDigitisersBlock(): DigitiserPatchBlock | undefined {
    // Only include newly added digitisers (those marked as new)
    const add = (this.digitisers.value as DigitiserFormValue[])
      .filter((d) => d.isNew && d.dasscoUserId !== null && d.dasscoUserId !== undefined)
      .map((d) => {
        const dasscoUserId = d.dasscoUserId;
        if (dasscoUserId === null || dasscoUserId === undefined) {
          return null;
        }
        return {
          dasscoUserId,
          assetGuids: d.assetGuids ?? this.data.assets.map((asset) => asset.asset_guid as string)
        };
      })
      .filter((d): d is DigitiserAddAction => d !== null);

    const deleteIds = this.deletedDigitiserIds.length > 0 ? this.deletedDigitiserIds : undefined;

    if (add.length === 0 && !deleteIds) {
      return undefined;
    }

    return {
      ...(add.length > 0 && {add}),
      ...(deleteIds && {delete: deleteIds})
    };
  }

  get fields() {
    return this.bulkUpdateForm.controls.fields as FormGroup;
  }

  get statusControl() {
    return this.fields.controls['status'] as FormControl<string | null>;
  }

  get subjectControl() {
    return this.fields.controls['subject'] as FormControl<string | null>;
  }

  get digitiserIdControl() {
    return this.fields.controls['digitiser_id'] as FormControl<number | null>;
  }

  get issues() {
    return this.bulkUpdateForm.controls.issues as FormArray;
  }

  get digitisers() {
    return this.bulkUpdateForm.controls.digitisers as FormArray;
  }

  get legality() {
    return this.bulkUpdateForm.controls.legality as FormGroup;
  }

  bulkUpdateForm = new FormGroup({
    fields: new FormGroup({
      asset_locked: new FormControl<boolean | null>(null),
      audited: new FormControl<boolean | null>(null),
      subject: new FormControl<string | null>(null),
      status: new FormControl<string | null>(null),
      digitiser_id: new FormControl<number | null>(null),
      camera_setting_control: new FormControl<string | null>(null),
      metadata_source: new FormControl<string | null>(null),
      push_to_specify: new FormControl<boolean | null>(null),
      role_restrictions: new FormControl<string[] | null>(null),
      payload_type: new FormControl<string | null>(null)
    }),
    funding: new FormControl<number[] | null>(null),
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
    digitisers: new FormArray<
      FormGroup<{
        dasscoUserId: FormControl<number | null>;
        username: FormControl<string | null>;
        digitiserListIds: FormControl<number[] | null>;
        assetGuids: FormControl<string[] | null>;
        count: FormControl<number | null>;
        isNew: FormControl<boolean | null>;
      }>
    >([]),
    legality: new FormGroup({
      copyright: new FormControl<string | null>(null),
      license: new FormControl<string | null>(null),
      credit: new FormControl<string | null>(null)
    })
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
