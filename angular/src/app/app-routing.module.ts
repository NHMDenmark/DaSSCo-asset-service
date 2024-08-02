import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {UserComponent} from './components/user/user.component';
import {AuthenticatedGuard} from './authenticated.guard';
import {DocsComponent} from './components/docs/docs.component';
import {GraphComponent} from './components/graph/graph.component';
import {StatisticsComponent} from './components/statistics/statistics.component';

const routes: Routes = [
  {path: 'statistics', component: StatisticsComponent, canActivate: [AuthenticatedGuard]},
  {path: 'graphs', component: GraphComponent, canActivate: [AuthenticatedGuard]},
  {path: 'user', component: UserComponent, canActivate: [AuthenticatedGuard]},
  {path: 'docs', component: DocsComponent, canActivate: [AuthenticatedGuard]},
  {path: '**', redirectTo: 'statistics', pathMatch: 'prefix'}

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
