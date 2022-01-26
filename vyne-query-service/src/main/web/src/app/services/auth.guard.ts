import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { VyneUser, UserInfoService } from './user-info.service';
import {map} from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private router: Router,
    private userInfoService: UserInfoService
  ) { }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const currentUser = this.userInfoService.userInfo$.getValue();
    if (currentUser) {
      return this.checkAuthorisation(currentUser, route);
    } else {
      console.log("getting user info for route => " + route.url[0]);
      return this.userInfoService.getUserInfo(true).pipe(map(
        (vyneUser: VyneUser) => {
          return this.checkAuthorisation(vyneUser, route);
        }
      ));
    }
    return false;
  }

  private checkAuthorisation(vyneUser: VyneUser, route: ActivatedRouteSnapshot) {
    if (route.data.grantedAuthority && vyneUser.grantedAuthorities.indexOf(route.data.grantedAuthority) === -1) {
      // role not authorised so redirect to home page
      this.router.navigate(['/']);
      return false;
    }

    // authorised so return true
    return true;
  }
}
