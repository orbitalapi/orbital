import {Component} from '@angular/core';
import {BreakpointObserver, Breakpoints} from '@angular/cdk/layout';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {AppInfo, AppInfoService} from "../services/app-info.service";

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
      icon: 'explore',
      // icon: 'outline-explore.svg',
      // iconActive: 'outline-explore-active.svg',
      route: 'type-explorer'
    },
    {
      title: 'Query builder',
      icon: 'layers',
      // icon: 'outline-layers.svg',
      // iconActive: 'outline-layers-active.svg',
      route: 'query-wizard'
    },
    {
      title: 'Query history',
      icon: 'history',
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
    }
  });

  appInfo: AppInfo;

  constructor(private breakpointObserver: BreakpointObserver, appInfoService: AppInfoService) {
    appInfoService.getAppInfo()
      .subscribe(info => this.appInfo = info)
  }

}


export interface SidebarElement {
  title: string;
  icon: string;
  iconActive: string;
  route: string
}
