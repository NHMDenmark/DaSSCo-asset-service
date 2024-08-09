import { Injectable } from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {Asset} from "../types/types";

@Injectable({
  providedIn: 'root'
})
export class QueryToOtherPages {

  private assets : string[] = [];
  private fullAsset : Asset[] = [];
  private dataSource : MatTableDataSource<Asset> = new MatTableDataSource<Asset>();

  setAssets(assets : string[]) : void{
    this.assets = assets;
  }

  setDataSource(dataSource : MatTableDataSource<Asset>){
    this.dataSource = dataSource;
  }

  setFullAssets(assets : Asset[]) : void {
    this.fullAsset = assets;
  }

  getAssets(): string[]{
    return this.assets;
  }

  getDataSource() : MatTableDataSource<Asset> {
    return this.dataSource;
  }

  getFullAssets() : Asset[] {
    return this.fullAsset;
  }

  constructor() { }
}
