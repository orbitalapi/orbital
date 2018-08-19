import {Component} from '@angular/core';
import {BreakpointObserver, Breakpoints} from '@angular/cdk/layout';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

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
      icon: 'outline-explore.svg',
      iconActive: 'outline-explore-active.svg',
      route: 'type-explorer'
    },
    {
      title: 'Query builder',
      icon: 'outline-layers.svg',
      iconActive: 'outline-layers-active.svg',
      route: 'query-wizard'
    },
  ].map(value => {
    return {
      title: value.title,
      icon: `assets/img/${value.icon}`,
      iconActive: `assets/img/${value.iconActive}`,
      route: value.route
    }
  });

  constructor(private breakpointObserver: BreakpointObserver) {
  }

}


export interface SidebarElement {
  title: string;
  icon: string;
  iconActive: string;
  route: string
}
