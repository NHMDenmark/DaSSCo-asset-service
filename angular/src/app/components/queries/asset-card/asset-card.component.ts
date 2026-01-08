import {Component, EventEmitter, inject, Input, Output} from '@angular/core';
import {BehaviorSubject} from 'rxjs';
import {AssetService} from 'src/app/services/asset.service';
import {AssetSpecimen, QueryResultAsset, Specimen} from 'src/app/types/types';
@Component({
  selector: 'dassco-asset-card',
  host: {
    style: 'display: flex; max-width: 360px; min-width: 272px; flex: 1 1 320px; flex-direction: column;'
  },
  templateUrl: './asset-card.component.html',
  styleUrls: ['./asset-card.component.scss']
})
export class AssetCardComponent {
  @Input() set asset(asset: QueryResultAsset) {
    this.currentAsset.next(asset);
  }
  @Input() checked = false;
  @Output() toggle = new EventEmitter<QueryResultAsset>();
  @Output() onClick = new EventEmitter<QueryResultAsset>();
  assetService = inject(AssetService);
  currentAsset = new BehaviorSubject<QueryResultAsset | undefined>(undefined);
  asset$ = this.currentAsset.asObservable();

  assetSpecimenToSpecimen(assetSpecimen: AssetSpecimen[]): Specimen[] {
    if (!assetSpecimen) {
      return [] as Specimen[];
    }
    return assetSpecimen.flatMap((a) => a.specimen).filter((value) => !!value) as Specimen[];
  }
}
