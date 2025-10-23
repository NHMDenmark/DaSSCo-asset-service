import {inject, Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {map, Observable} from 'rxjs';
import {ExternDetailedViewService} from "../services/extern-detailed-view.service";

@Injectable({
  providedIn: 'root'
})
export class HasRightsGuard implements CanActivate {
  router = inject(Router);
  externDetailedViewService = inject(ExternDetailedViewService);

  canActivate(
    route: ActivatedRouteSnapshot,
    _state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {


    const assetGuid = route.paramMap.get("asset_guid");
    if (!assetGuid) {
      return this.router.parseUrl("/ars")
    }
    return this.externDetailedViewService.checkAccess(assetGuid).pipe(
      map((result) => result ? true : this.router.parseUrl(`/ars/detailed-view/${assetGuid}`))
    )
  }

}
