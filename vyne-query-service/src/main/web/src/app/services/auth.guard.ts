import {Injectable} from '@angular/core';
import {Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {VyneUser, UserInfoService} from './user-info.service';
import {map} from 'rxjs/operators';
import {AuthService} from "../auth/auth.service";

@Injectable({providedIn: 'root'})
export class AuthGuard implements CanActivate {
  constructor(
    private router: Router,
    private authService: AuthService,
    private userInfoService: UserInfoService
  ) {
  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    return this.authService.canActivateProtectedRoutes$.pipe(map(canAuthorize => {
      if (canAuthorize === true) {
        return this.checkAuthorisation(this.userInfoService.userInfo$.getValue(), route);
      } else {
        return false;
      }
    }))
  }

  private checkAuthorisation(vyneUser: VyneUser, route: ActivatedRouteSnapshot) {
    if (route.data.requiredAuthority && vyneUser.grantedAuthorities.includes(route.data.requiredAuthority)) {
      // authorised so return true
      return true;
    }
    // role not authorised so redirect to home page
    this.router.navigate(['/']);
    return false;
  }
}
