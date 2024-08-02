import { Injectable } from '@angular/core';
import {MatTableDataSource} from "@angular/material/table";
import {Asset} from "../types/types";

@Injectable({
  providedIn: 'root'
})
export class QueryToOtherPages {

  private assets : string[] = [];
  private dataSource : MatTableDataSource<Asset> = new MatTableDataSource<Asset>();

  setAssets(assets : string[]) : void{
    this.assets = assets;
  }

  setDataSource(dataSource : MatTableDataSource<Asset>){
    this.dataSource = dataSource;
  }

  getAssets(): string[]{
    return this.assets;
  }

  getDataSource() : MatTableDataSource<Asset> {
    return this.dataSource;
  }

  constructor() { }
}
