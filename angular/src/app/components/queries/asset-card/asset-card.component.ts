import {Component, EventEmitter, inject, Input, Output} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {AssetService} from 'src/app/services/asset.service';
import {Asset, AssetSpecimen, Specimen} from 'src/app/types/types';
@Component({
  selector: 'dassco-asset-card',
  host: {
    style: 'display: flex; max-width: 360px; min-width: 272px; flex: 1 1 320px; flex-direction: column;'
  },
  templateUrl: './asset-card.component.html',
  styleUrls: ['./asset-card.component.scss']
})
export class AssetCardComponent {
  @Input() set asset(asset: Asset) {
    this.currentAsset.next(asset);
  }
  @Input() checked = false;
  @Output() toggle = new EventEmitter<Asset>();
  @Output() onClick = new EventEmitter<Asset>();
  assetService = inject(AssetService);
  currentAsset = new BehaviorSubject<Asset | undefined>(undefined);
  asset$ = this.currentAsset.asObservable();

  assetSpecimenToSpecimen(assetSpecimen: AssetSpecimen[]): Specimen[] {
    if (!assetSpecimen) {
      return [] as Specimen[];
    }
    return assetSpecimen.flatMap((a) => a.specimen).filter((value) => !!value) as Specimen[];
  }
}
