import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {UserComponent} from './components/user/user.component';
import {AuthConfigModule} from './auth-config.module';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {StatisticsComponent} from './components/statistics/statistics.component';
import {AssetsComponent} from './components/assets/assets.component';
import {GraphComponent} from './components/graph/graph.component';
import {AssetDetailComponent} from './components/asset-detail/asset-detail.component';
import {MatInputModule} from '@angular/material/input';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {DocsComponent} from './components/docs/docs.component';
import {ChartComponent} from './components/chart/chart.component';
import {GraphDataComponent} from './components/graph-data/graph-data.component';
import {MatNativeDateModule, MatOptionModule, MatRippleModule} from '@angular/material/core';
import {MatSelectModule} from '@angular/material/select';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatMomentDateModule} from '@angular/material-moment-adapter';
import {MatTooltipModule} from "@angular/material/tooltip";
import { ExportChartComponent } from './components/export-chart/export-chart.component';
import {MatSnackBarModule} from "@angular/material/snack-bar";
import { StatusWidgetComponent } from './components/status-widget/status-widget.component';
import {MatDividerModule} from "@angular/material/divider";
import {MatTableModule} from "@angular/material/table";
import {BulkUpdateComponent} from "./components/bulk-update/bulk-update.component";
import {MatChipsModule} from "@angular/material/chips";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {MatExpansionModule} from "@angular/material/expansion";
import { QueriesComponent } from './components/queries/queries.component';
import {MatCardModule} from "@angular/material/card";
import { QueryBuilderComponent } from './components/query-builder/query-builder.component';
import {DetailedViewComponent} from "./components/detailed-view/detailed-view.component";
import {MatListModule} from "@angular/material/list";
import { QueryHandlerComponent } from './components/query-handler/query-handler.component';
import {MatDialogModule} from "@angular/material/dialog";
import { SavedSearchesDialogComponent } from './components/dialogs/saved-searches-dialog/saved-searches-dialog.component';
import { SaveSearchDialogComponent } from './components/dialogs/save-search-dialog/save-search-dialog.component';
import {MatPaginatorModule} from "@angular/material/paginator";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {MatAutocompleteModule} from "@angular/material/autocomplete";
import {MatMenuModule} from "@angular/material/menu";
import {MatSortModule} from "@angular/material/sort";
import { AssetGroupDialogComponent } from './components/dialogs/asset-group-dialog/asset-group-dialog.component';
import { AssetGroupsComponent } from './components/asset-groups/asset-groups.component';
import { NewGroupDialogComponent } from './components/dialogs/new-group-dialog/new-group-dialog.component';
import { IllegalAssetGroupDialogComponent } from './components/dialogs/illegal-asset-group-dialog/illegal-asset-group-dialog.component';
import {NgOptimizedImage} from "@angular/common";

@NgModule({
  declarations: [
    AppComponent,
    UserComponent,
    DocsComponent,
    BulkUpdateComponent,
    StatisticsComponent,
    AssetsComponent,
    GraphComponent,
    AssetDetailComponent,
    ChartComponent,
    GraphDataComponent,
    ExportChartComponent,
    StatusWidgetComponent,
    QueriesComponent,
    QueryBuilderComponent,
    QueryHandlerComponent,
    SavedSearchesDialogComponent,
    SaveSearchDialogComponent,
    DetailedViewComponent,
    AssetGroupDialogComponent,
    AssetGroupsComponent,
    NewGroupDialogComponent,
    IllegalAssetGroupDialogComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    AuthConfigModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    FormsModule,
    BrowserAnimationsModule,
    MatOptionModule,
    MatSelectModule,
    ReactiveFormsModule,
    MatButtonToggleModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatMomentDateModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatDividerModule,
    MatTableModule,
    MatCardModule,
    MatExpansionModule,
    MatDialogModule,
    MatChipsModule,
    MatCheckboxModule,
    MatExpansionModule,
    MatCardModule,
    MatListModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatAutocompleteModule,
    MatMenuModule,
    MatSortModule,
    MatRippleModule,
    NgOptimizedImage
  ],
  schemas: [
    CUSTOM_ELEMENTS_SCHEMA
  ],
  providers: [
    MatDatepickerModule,
    ChartComponent
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
