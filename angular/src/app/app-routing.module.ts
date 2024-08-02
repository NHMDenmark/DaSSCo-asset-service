import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {UserComponent} from './components/user/user.component';
import {AuthenticatedGuard} from './authenticated.guard';
import {DocsComponent} from './components/docs/docs.component';
import {GraphComponent} from './components/graph/graph.component';
import {StatisticsComponent} from './components/statistics/statistics.component';

const routes: Routes = [
  {path: 'ars/statistics', component: StatisticsComponent, canActivate: [AuthenticatedGuard]},
  {path: 'ars/graphs', component: GraphComponent, canActivate: [AuthenticatedGuard]},
  {path: 'ars/user', component: UserComponent, canActivate: [AuthenticatedGuard]},
  {path: 'ars/docs', component: DocsComponent, canActivate: [AuthenticatedGuard]},
  {path: '**', redirectTo: 'ars/statistics', pathMatch: 'full'}

];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
