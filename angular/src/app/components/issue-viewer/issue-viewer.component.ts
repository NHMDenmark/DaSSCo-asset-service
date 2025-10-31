import {DIALOG_DATA} from '@angular/cdk/dialog';
import {Component, inject} from '@angular/core';
import {MatDialogRef} from '@angular/material/dialog';
import {Issue} from 'src/app/types/types';

@Component({
  selector: 'dassco-issue-viewer',
  templateUrl: './issue-viewer.component.html',
  styleUrls: ['./issue-viewer.component.scss']
})
export class IssueViewerComponent {
  dialogRef = inject(MatDialogRef);
  issue: Issue = inject(DIALOG_DATA);
}
