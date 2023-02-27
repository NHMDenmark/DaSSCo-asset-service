import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {UserComponent} from "./user/user.component";
import {AuthenticatedGuard} from "./authenticated.guard";
import {DocsComponent} from "./docs/docs.component";
import {AssetsComponent} from "./assets/assets.component";
import {GraphComponent} from "./graph/graph.component";
import {StatisticsComponent} from "./statistics/statistics.component";

const routes: Routes = [
  {path: 'assets', component: AssetsComponent, canActivate: [AuthenticatedGuard]},
  {path: 'statistics', component: StatisticsComponent, canActivate: [AuthenticatedGuard]},
  {path: 'graphs', component: GraphComponent, canActivate: [AuthenticatedGuard]},
  {path: 'user', component: UserComponent, canActivate: [AuthenticatedGuard]},
  {path: 'docs', component: DocsComponent, canActivate: [AuthenticatedGuard]},
  {path: '**', redirectTo: 'assets', pathMatch: 'full'},

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
