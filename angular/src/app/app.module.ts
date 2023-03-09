import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { UserComponent } from './components/user/user.component';
import {AuthConfigModule} from "./auth-config.module";
import {MatToolbarModule} from "@angular/material/toolbar";
import {MatIconModule} from "@angular/material/icon";
import {MatButtonModule} from "@angular/material/button";
import { StatisticsComponent } from './components/statistics/statistics.component';
import { AssetsComponent } from './components/assets/assets.component';
import { GraphComponent } from './components/graph/graph.component';
import { AssetDetailComponent } from './components/asset-detail/asset-detail.component';
import {MatInputModule} from "@angular/material/input";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {DocsComponent} from "./components/docs/docs.component";
import { LineChartComponent } from './components/line-chart/line-chart.component';
import { GraphDataComponent } from './components/graph-data/graph-data.component';
import {MatOptionModule} from "@angular/material/core";
import {MatSelectModule} from "@angular/material/select";
import {MatButtonToggleModule} from "@angular/material/button-toggle";
import { MultiChartComponent } from './components/multi-chart/multi-chart.component';

@NgModule({
  declarations: [
    AppComponent,
    UserComponent,
    DocsComponent,
    StatisticsComponent,
    AssetsComponent,
    GraphComponent,
    AssetDetailComponent,
    LineChartComponent,
    GraphDataComponent,
    MultiChartComponent
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
    MatButtonToggleModule
  ],
  schemas: [
    CUSTOM_ELEMENTS_SCHEMA
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
