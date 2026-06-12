import {Component, inject} from '@angular/core';
import {AssetBundleDownload, AssetBundleDownloadService} from '../../services/asset-bundle-download.service';

@Component({
  selector: 'dassco-asset-bundle-download-status',
  templateUrl: './asset-bundle-download-status.component.html',
  styleUrls: ['./asset-bundle-download-status.component.scss']
})
export class AssetBundleDownloadStatusComponent {
  readonly downloadService = inject(AssetBundleDownloadService);

  cancelOrDismiss(download: AssetBundleDownload): void {
    this.downloadService.cancel(download);
  }
}
