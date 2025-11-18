import {DIALOG_DATA, DialogRef} from '@angular/cdk/dialog';
import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {MatIconModule} from '@angular/material/icon';
import {SafeUrl} from '@angular/platform-browser';
import {MatButtonModule} from '@angular/material/button';

@Component({
  selector: 'dassco-asset-thumbnail-modal',
  template: `
    <div>
      <button tabindex="-1" type="button" mat-icon-button (click)="dialogRef.close()">
        <mat-icon>close</mat-icon>
      </button>
      <img [src]="thumbnail" alt="Asset thumbnail" />
    </div>
  `,
  standalone: true,
  styles: [
    'div { border-radius: 0.375rem; background-color: white; display: flex; padding: 1rem; border: 1px solid #ccc; max-height: 99dvh; position: relative}',
    'button[type="button"] { position: absolute; right: -5px; top: -5px; } ',
    'img { object-fit: contain; }'
  ],
  imports: [MatIconModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetThumbnailModalComponent {
  dialogRef = inject(DialogRef);

  thumbnail: SafeUrl = inject(DIALOG_DATA);
}
