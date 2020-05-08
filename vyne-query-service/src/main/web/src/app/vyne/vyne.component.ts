import {Component} from '@angular/core';
import {BreakpointObserver, Breakpoints} from '@angular/cdk/layout';
import {Observable} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {AppInfo, AppInfoService} from '../services/app-info.service';
import {NavigationEnd, Router} from '@angular/router';

@Component({
  selector: 'vyne-app',
  templateUrl: './vyne.component.html',
  styleUrls: ['./vyne.component.scss']
})
export class VyneComponent {

  isHandset$: Observable<boolean> = this.breakpointObserver.observe(Breakpoints.Handset)
    .pipe(
      map(result => result.matches)
    );

  sidebarElements: SidebarElement[] = [
    {
      title: 'Type explorer',
      icon: 'assets/img/class.svg',
      // icon: 'explore',
      // icon: 'outline-explore.svg',
      // iconActive: 'outline-explore-active.svg',
      route: 'types'
    },
    {
      title: 'Schema explorer',
      icon: 'assets/img/coding.svg',
      route: 'schema-explorer'
    },
    {
      title: 'Query builder',
      icon: 'assets/img/query.svg',
      // icon: 'outline-layers.svg',
      // iconActive: 'outline-layers-active.svg',
      route: 'query-wizard'
    },
    {
      title: 'Query history',
      icon: 'assets/img/history.svg',
      route: 'query-history'
    }

  ].map(value => {
    return {
      title: value.title,
      // icon: `assets/img/${value.icon}`,
      // iconActive: `assets/img/${value.icon}`,
      icon: value.icon,
      iconActive: value.icon,
      route: value.route
    };
  });

  appInfo: AppInfo;

  constructor(private breakpointObserver: BreakpointObserver, appInfoService: AppInfoService, private router: Router) {
    appInfoService.getAppInfo()
      .subscribe(info => this.appInfo = info);

    // When the user navigates using the router, scroll back to the top.
    // Won't always be appropriate, (ie., when there are anchor links),
    // but it's right more often than it's not.
    // Based on
    // https://github.com/angular/components/issues/4280#issuecomment-300703342
    // Unfortunately, native angular support doesn't work until maybe v9.x or 10
    // https://github.com/angular/angular/issues/24547

    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => {
        document.querySelector('.mat-sidenav-content').scrollTop = 0;
      });
  }

  onNavigationEvent() {
  }
}


export interface SidebarElement {
  title: string;
  icon: string;
  iconActive: string;
  route: string;
}
