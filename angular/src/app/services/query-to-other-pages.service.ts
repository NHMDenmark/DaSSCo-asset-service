import {Injectable} from '@angular/core';
import {MatTableDataSource} from '@angular/material/table';
import {Asset, QueryResultAsset} from '../types/types';

@Injectable({
  providedIn: 'root'
})
export class QueryToOtherPages {
  private assets: string[] = [];
  private fullAsset: Asset[] = [];
  private dataSource: MatTableDataSource<QueryResultAsset> = new MatTableDataSource<QueryResultAsset>();

  setAssets(assets: string[]): void {
    this.assets = assets;
  }

  setDataSource(dataSource: MatTableDataSource<QueryResultAsset>) {
    this.dataSource = dataSource;
  }

  setFullAssets(assets: Asset[]): void {
    this.fullAsset = assets;
  }

  getAssets(): string[] {
    return this.assets;
  }

  getDataSource(): MatTableDataSource<QueryResultAsset> {
    return this.dataSource;
  }

  getFullAssets(): Asset[] {
    return this.fullAsset;
  }

  constructor() {
    const currentAssetsFromQuery = sessionStorage.getItem('currentAssetsFromQuery');
    if (currentAssetsFromQuery) {
      try {
        const assets = JSON.parse(currentAssetsFromQuery) as string[];
        this.setAssets(assets);
      } catch {
        console.error("Couldn't parse assets guids from session storage");
      }
    }
  }
}
