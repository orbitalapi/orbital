import { Injectable } from '@angular/core';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { map } from 'rxjs/operators';
import { AppInfoService } from './app-info.service';

@Injectable({providedIn: 'root'})
export class FeatureFlagGuard {
  constructor(
    private router: Router,
    private appConfigService: AppInfoService,
  ) {
  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    return this.appConfigService.getConfig().pipe(map(appConfig => {
      const {requiredFeatureFlag} = route.data;
      return appConfig.featureToggles[requiredFeatureFlag] === true;
    }))
  }
}
