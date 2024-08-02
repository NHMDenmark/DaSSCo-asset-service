import {AfterViewInit, Component, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {QueriesService} from "../../services/queries.service";
import {filter, iif, map, Observable, of, take} from "rxjs";
import {isNotUndefined} from "@northtech/ginnungagap";
import {Query, QueryView, QueryWhere, QueryResponse} from "../../types/query-types";
import {MatTableDataSource} from "@angular/material/table";
import {QueryHandlerComponent} from "../query-handler/query-handler.component";
import {
  SavedSearchesDialogComponent
} from "../dialogs/saved-searches-dialog/saved-searches-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {SaveSearchDialogComponent} from "../dialogs/save-search-dialog/save-search-dialog.component";
import {MatSnackBar} from "@angular/material/snack-bar";
import {MatPaginator} from "@angular/material/paginator";
import {CacheService} from "../../services/cache.service";
import {Asset, AssetGroup, DasscoError} from "../../types/types";
import {MatSort, Sort} from "@angular/material/sort";
import {SelectionModel} from "@angular/cdk/collections";
import {AssetGroupDialogComponent} from "../dialogs/asset-group-dialog/asset-group-dialog.component";
import {AssetGroupService} from "../../services/asset-group.service";
import {DetailedViewService} from "../../services/detailed-view.service";
import {
  IllegalAssetGroupDialogComponent
} from "../dialogs/illegal-asset-group-dialog/illegal-asset-group-dialog.component";
import {QueryToDetailedViewService} from "../../services/query-to-detailed-view.service";
import {Router} from "@angular/router";

@Component({
  selector: 'dassco-queries',
  templateUrl: './queries.component.html',
  styleUrls: ['./queries.component.scss']
})
export class QueriesComponent implements OnInit, AfterViewInit {
  @ViewChild('queryHandlerContainer', { read: ViewContainerRef, static: true }) queryHandlerEle: ViewContainerRef | undefined;
  @ViewChild(MatPaginator) paginator: MatPaginator | undefined;
  @ViewChild(MatSort) set matSort(sort: MatSort) {
    this.dataSource.sort = sort;
  }

  displayedColumns: string[  ] = ['select', 'asset_guid', 'institution', 'collection', 'barcode', 'file_formats', 'created_date', 'events'];
  selection = new SelectionModel<Asset>(true, []);
  dataSource = new MatTableDataSource<Asset>();
  limit: number = 200;
  queries: Map<string, QueryView[]> = new Map;
  nodes: Map<string, string[]> = new Map();
  queryUpdatedTitle: string | undefined;
  loadingAssetCount: boolean = false;
  assetCount: string | undefined = undefined;
  queryData: {title: string | undefined, map: Map<string, QueryView[]>} | undefined; // saved/loaded or cached
  selectedAssets = new Set<string>();

  propertiesCall$: Observable<Map<string, string[]> | undefined>
    = this.queriesService.nodeProperties$
    .pipe(
      filter(isNotUndefined),
      map(nodes => {
        this.cacheService.setNodeProperties(nodes);
        this.nodes = nodes;
        return new Map(Object.entries(nodes));
      })
    )

  propertiesCached$
    = of (this.cacheService.getNodeProperties()).pipe(
    map(properties => {
      if (properties) {
        this.nodes = properties;
        return properties;
      }
      return new Map();
    })
  )

  nodes$: Observable<Map<string, string[]> | undefined>
  = iif(() => { // is this the "best" way of doing it? no clue. but it works. ¯\_(ツ)_/¯
      return this.cacheService.getNodeProperties() == undefined;
    },
    this.propertiesCall$, // if it's undefined
    this.propertiesCached$ // if it's not undefined
  );

  constructor(private queriesService: QueriesService
              , public dialog: MatDialog
              , private _snackBar: MatSnackBar
              , private cacheService: CacheService
              , private assetGroupService: AssetGroupService
              , private queryToDetailedView : QueryToDetailedViewService
              , private router : Router,
              private detailedViewService : DetailedViewService
  ) { }

  ngOnInit(): void {
    this.nodes$.pipe(filter(isNotUndefined),take(1))
      .subscribe(_nodes => {
        const cachedQueries = this.cacheService.getQueries();

        if (cachedQueries) {
          this.queryData = cachedQueries;
          this.addSelectFromData(this.queryData.map);
          this.queries = this.queryData.map;
          this.queryUpdatedTitle = this.queryData.title;
        } else {
          this.newSelect(undefined);
        }
      })

    if (this.queryToDetailedView.getDataSource().filteredData.length > 0){
      this.dataSource = this.queryToDetailedView.getDataSource();
    }
  }

  ngAfterViewInit(): void {
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
  }

  newSelect(savedQuery: QueryView[] | undefined) {
    if (this.queryHandlerEle) {
      const handlerComponent = this.queryHandlerEle.createComponent(QueryHandlerComponent, {index: this.queryHandlerEle.length});
      handlerComponent.instance.nodes = this.nodes;
      handlerComponent.instance.savedQuery = savedQuery;
      const childIdx = this.queryHandlerEle!.indexOf(handlerComponent.hostView);
      handlerComponent.instance.idx = childIdx;
      handlerComponent.instance.saveQueryEvent.subscribe(queries => {
        this.queries.set(childIdx.toString(), queries);
        this.cacheService.setQueries({title: this.queryData && this.queryData.title ? this.queryUpdatedTitle : undefined, map: this.queries})
      });
      handlerComponent.instance.removeComponentEvent.subscribe(() => {
        this.queries.delete(childIdx.toString());
        handlerComponent.destroy();
      });
    }
  }

  saveQuery(queries: QueryView[], index: number) {
    this.queries.set(index.toString(), queries);
  }

  save() {
    const queryResponses: QueryResponse[] = [];

    this.queries.forEach((val, key) => {
      const nodeMap = new Map<string, QueryWhere[]>;
      val.forEach(where => {
        if (nodeMap.has(where.node)) {
          nodeMap.get(where.node)!.push({property: where.property, fields: where.fields});
        } else {
          nodeMap.set(where.node, [{property: where.property, fields: where.fields}]);
        }
      })
      const qv2s = Array.from(nodeMap).map((value) => {
        return {select: value[0], where: value[1]} as Query;
      })

      const response: QueryResponse = {id: parseInt(key), query: qv2s};
      queryResponses.push(response);
      this.selection.clear();
    })

    this.queriesService.getAssetsFromQuery(queryResponses, this.limit)
      .subscribe(result => {
        if (result) {
          this.dataSource.data = result;
        }
      })

    this.loadingAssetCount = true;
    this.queriesService.getAssetCountFromQuery(queryResponses, this.limit)
      .subscribe(count => {
        if (count != undefined) {
          if (count >= 10000) this.assetCount = '10000+';
          else this.assetCount = count.toString();
          this.loadingAssetCount = false;
        }
      })
  }

  clearAll() {
    this.queryHandlerEle?.clear();
    this.queries.clear();
    this.dataSource.data = [];
    this.queryData = undefined;
    this.assetCount = undefined;
    this.cacheService.clearQueryCache();
    this.newSelect(undefined);
    this.queryToDetailedView.setDataSource(new MatTableDataSource<Asset>());
  }

  saveSearch() {
    const dialogRef = this.dialog.open(SaveSearchDialogComponent);

    dialogRef.afterClosed().subscribe((title: string | undefined) => {
      if (title) {
        this.queriesService.saveSearch({name: title, query: JSON.stringify(Object.fromEntries(this.queries))})
          .subscribe(saved => {
            this.openSnackBar(saved, 'The search "' + title + '" has been saved.')
          })
      }
    });
  }

  savedSearchesDialog(): void {
    const dialogRef = this.dialog.open(SavedSearchesDialogComponent, {
      width: '300px'
    });

    // deleting a saved query
    dialogRef.componentInstance.deleteQuery$
      .pipe(filter(isNotUndefined))
      .subscribe(queryName => {
        this.queriesService.deleteSavedSearch(queryName)
          .subscribe(deleted => {
            this.openSnackBar(deleted, 'The search has been deleted.')
          })
      });

    // opening saved query
    dialogRef.afterClosed().subscribe((queryMap: {title: string, map: Map<string, QueryView[]>} | undefined) => {
      this.queryData = queryMap;
      if (queryMap) {
        this.queryUpdatedTitle = queryMap.title;
        this.queryHandlerEle?.clear();
        this.dataSource.data = [];
        this.addSelectFromData(queryMap.map);
      }
    });
  }

  addSelectFromData(query: Map<string, QueryView[]>) {
    Array.from(query.keys()).forEach((key) => {
      this.newSelect(query.get(key));
    });
  }

  updateSearch() {
    if (this.queryUpdatedTitle && this.queryData?.title) {
      this.queriesService.updateSavedSearch({name: this.queryUpdatedTitle, query: JSON.stringify(Object.fromEntries(this.queries))}, this.queryData.title)
        .subscribe(updated => {
          if (this.queryData) this.queryData.title = updated?.name;
          this.queryUpdatedTitle = updated?.name;
          this.cacheService.setQueryTitle(updated?.name);
        })
    }
  }

  filterFileFormats(event: Event) {
    console.log(event)
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  sortData(sort: Sort) {
    if (sort.active == 'barcode') {
      const data = this.dataSource.data;

      const sortedData = data.sort((a, b) => {
        const isAsc = sort.direction === 'asc';
        if (a.specimens && b.specimens) {
          return this.compare(a.specimens[0].barcode, b.specimens[0].barcode, isAsc);
        }
        return 0;
      })
      this.dataSource.data = sortedData;
    }
  }

  compare(a: number | string | undefined, b: number | string | undefined, isAsc: boolean) {
    if (a && b) {
      return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
    }
    return 0;
  }

  isAllSelected() { // assets selection
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  toggleAllRows() { // asset rows
    if (this.isAllSelected()) {
      this.selection.clear();
      return;
    }
    this.selection.select(...this.dataSource.data);
  }

  addToGroup() {
    const dialogRef = this.dialog.open(AssetGroupDialogComponent, {
      width: '500px'
    });

    dialogRef.afterClosed().subscribe((group: {group: AssetGroup, new: boolean}) => {
      if (group) {
        group.group.assets = this.selection.selected.map(asset => asset.asset_guid).filter(isNotUndefined);
        if (group.new) {
          this.assetGroupService.newGroup(group.group)
            .subscribe(response => {
              if ((response as DasscoError).errorMessage) {
                const error = response as DasscoError;
                this.dialog.open(IllegalAssetGroupDialogComponent, {
                  width: '500px',
                  data: {
                    assets: error.body,
                    removable: false
                  }
                });
              } else if ((response as AssetGroup).group_name) {
                this._snackBar.open('The group "' + group.group.group_name + '" has been created.', 'OK');
              }
            });
        } else {
          this.assetGroupService.updateGroupAddAssets(group.group.group_name, group.group.assets)
            .subscribe(response => {
              if ((response as DasscoError).errorMessage) {
                const error = response as DasscoError;
                this.dialog.open(IllegalAssetGroupDialogComponent, {
                  width: '500px',
                  data: {
                    assets: error.body,
                    removable: false
                  }
                });
              } else {
                this.openSnackBar(response, 'The group "' + group.group.group_name + '" has been updated.')
              }
            });
        }
      }
    });
  }

  openSnackBar(object: any | undefined, success: string) {
    if (object) {
      this._snackBar.open(success, 'OK');
    } else {
      this._snackBar.open('An error occurred. Try again.', 'OK');
    }
  }

  downloadCsv() {
    const assetGuids = this.selection.selected.map(asset => asset.asset_guid!)
    this.detailedViewService.postCsv(assetGuids)
      .subscribe({
        next: (response) => {
          if (response.status == 200){
            this.detailedViewService.getFile("assets.csv")
              .subscribe(
              {
                next: (data) => {
                  const url = window.URL.createObjectURL(data);
                  const link = document.createElement('a');
                  link.href = url;
                  link.download = "assets.csv";

                  document.body.appendChild(link);
                  link.click();

                  document.body.removeChild(link);
                  window.URL.revokeObjectURL(url);

                  this.detailedViewService.deleteFile()
                    .subscribe({
                      next: () => {
                      },
                      error: () => {
                        this.openSnackBar("There's been an error deleting the CSV file", "There's been an error deleting the CSV file")
                      }
                    })
                },
                error: () => {
                  this.openSnackBar("There has been an error downloading the CSV file.", "There has been an error downloading the CSV file.");
                }
              })
          }
        },
        error: (error) => {
          this.openSnackBar(error.error, error.error);
        }
      });
  }

  downloadZip() {
    const assetGuids = this.selection.selected.map(asset => asset.asset_guid!)
    this.detailedViewService.postCsv(assetGuids)
      .subscribe({
        next: (response) => {
          if (response.status == 200){
            this.detailedViewService.postZip(assetGuids)
              .subscribe({
                next: (response) => {
                  if (response.status == 200){
                    this.detailedViewService.getFile("assets.zip").subscribe(
                      {
                        next: (data) => {
                          const url = window.URL.createObjectURL(data);
                          const link = document.createElement('a');
                          link.href = url;
                          link.download = "assets.zip";

                          document.body.appendChild(link);
                          link.click();

                          document.body.removeChild(link);
                          window.URL.revokeObjectURL(url);

                          this.detailedViewService.deleteFile()
                            .subscribe({
                              next: () => {
                              },
                              error: () => {
                                this.openSnackBar("There has been an error deleting the files", "Close")
                              }
                            })
                        },
                        error: () => {
                          this.openSnackBar("There has been an error downloading the ZIP file.", "Close");
                        }
                      })
                  }
                },
                error: () => {
                  this.openSnackBar("There has been an error saving the files to the Temp Folder", "Close");
                }
              })
          }
        },
        error: () => {
          this.openSnackBar("There has been an error saving the CSV File", "Close");
        }
      });
  }

  areAssetsSelected() {
    return this.selection.hasValue();
  }

  assetClick(asset : string){
    // TODO: I need to get the clicked asset AND the entire list, so I can pass it to detailed view (in order!)
    const assetGuids : string[] = this.dataSource.filteredData.map(asset => asset.asset_guid!);

    this.queryToDetailedView.setAssets(assetGuids);
    this.queryToDetailedView.setDataSource(this.dataSource)

    this.router.navigate(['detailed-view/', asset])
  }
}
