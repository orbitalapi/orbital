import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TuiIcon, TuiHint } from '@taiga-ui/core';
import { BehaviorSubject, combineLatestWith } from 'rxjs';
import { Observable } from 'rxjs/internal/Observable';
import { filter, map, tap } from 'rxjs/operators';
import { HeaderBarModule } from '../header-bar/header-bar.module';
import { AppInfo, AppInfoService } from '../services/app-info.service';
import { UserInfoService, VynePrivileges, VyneUser } from '../services/user-info.service';

export interface SidebarElement {
  title: string;
  icon: string;
  iconActive: string;
  route?: string;
  requiredAuthority?: VynePrivileges;
  externalUrl?: string;
  testId?: string
  featureToggle?: string | null;
}

@Component({
  selector: 'app-sidenav',
  standalone: true,
  imports: [CommonModule, HeaderBarModule, RouterLink, RouterLinkActive, TuiIcon, TuiHint],
  templateUrl: './sidenav.component.html',
  styleUrls: ['./sidenav.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SidenavComponent implements OnInit {
  private sidebarElements: SidebarElement[] = [
    {
      title: 'Catalog',
      icon: 'assets/img/tabler/book2.svg',
      // icon: 'explore',
      // icon: 'outline-explore.svg',
      // iconActive: 'outline-explore-active.svg',
      route: 'catalog',
      testId: 'data-catalog-sidebar',
      requiredAuthority: VynePrivileges.BrowseSchema
    },
    {
      title: 'Projects',
      icon: 'assets/img/tabler/folder-code.svg',
      route: 'projects',
      testId: 'projects-explorer-sidebar',
      requiredAuthority: VynePrivileges.BrowseSchema
    },
    {
      title: 'Query editor',
      icon: 'assets/img/tabler/pencil-code.svg',
      // icon: 'outline-layers.svg',
      // iconActive: 'outline-layers-active.svg',
      route: 'query/editor',
      testId: 'query-builder-sidebar',
      requiredAuthority: VynePrivileges.RunQuery
    },
    {
      title: 'Query history',
      icon: 'assets/img/tabler/clock-code.svg',
      route: 'query-history',
      testId: 'query-history-sidebar',
      requiredAuthority: VynePrivileges.ViewQueryHistory
    },
    {
      title: 'Data sources',
      icon: 'assets/img/tabler/plug.svg',
      route: 'data-source-manager',
      requiredAuthority: VynePrivileges.ViewConnections
    },
    {
      title: 'Authentication',
      icon: 'assets/img/tabler/shield-lock.svg',
      route: 'authentication-manager',
      testId: 'authentication-sidebar',
      requiredAuthority: VynePrivileges.ViewAuthenticationTokens
    },
    {
      title: 'Model designer',
      icon: 'assets/img/tabler/tools.svg',
      route: 'designer',
      testId: 'designer',
      requiredAuthority: VynePrivileges.EditSchema
    },
    {
      title: 'Endpoints',
      icon: 'assets/img/tabler/broadcast.svg',
      route: 'endpoints',
      testId: 'endpoints',
      requiredAuthority: VynePrivileges.ViewPipelines
    },
    {
      title: 'Policies',
      icon: 'assets/img/tabler/lock-code.svg',
      route: 'policies',
      testId: 'policies',
      requiredAuthority: VynePrivileges.EditSchema,
      featureToggle: 'policiesEnabled'
    },
    {
      title: 'Stub Servers',
      icon: 'assets/img/tabler/server-spark.svg',
      route: 'stubs',
      testId: 'stubs',
      requiredAuthority: VynePrivileges.BrowseSchema,
      featureToggle: 'nebulaEnabled'
    },
  // TODO: check with Marty about the iconActive, and whether that's now not a thing (which it appears not to be)
  ].map(value => {
    return {
      title: value.title,
      icon: value.icon,
      iconActive: value.icon,
      route: value.route,
      testId: value.testId,
      requiredAuthority: value.requiredAuthority,
      featureToggle: value.featureToggle
    };
  });

  @Input()
  appInfo: AppInfo;

  @Input()
  customSidebarElements$: BehaviorSubject<SidebarElement[]>;

  sidebarElements$: Observable<SidebarElement[]>
  userInfo$: BehaviorSubject<VyneUser> = new BehaviorSubject(null);

  private _isCollapsed: boolean = true;
  get isCollapsed(): boolean {
    return this._isCollapsed;
  }
  set isCollapsed(value: boolean) {
    this._isCollapsed = value;
    localStorage.setItem(this.IS_COLLAPSED_LOCAL_STORAGE_KEY, value.toString());
  }

  private readonly IS_COLLAPSED_LOCAL_STORAGE_KEY: string = 'isSidebarCollapsed';
  private filteredSideBarElements$: Observable<SidebarElement[]>

  constructor(
    private userInfoService: UserInfoService,
    private appInfoService: AppInfoService
  ) {
  }

  ngOnInit(): void {
    this._isCollapsed = localStorage.getItem(this.IS_COLLAPSED_LOCAL_STORAGE_KEY) === 'true' ?? false;

    this.filteredSideBarElements$ = this.userInfoService.userInfo$
      .pipe(
        filter(userInfo => userInfo != null),
        tap(userInfo => this.userInfo$.next(userInfo)),
        map(userInfo => this.sidebarElements
          .filter(sideBarElement => userInfo.grantedAuthorities.includes(sideBarElement.requiredAuthority))
        ),
        combineLatestWith(this.appInfoService.getConfig()),
        map(([sidebarElements,config]) => {
          return sidebarElements.filter(sidebarElement => {
            return !sidebarElement.featureToggle ? true : config.featureToggles[sidebarElement.featureToggle];
          });
        })
      )

    this.sidebarElements$ = this.filteredSideBarElements$
      .pipe(
        combineLatestWith(this.customSidebarElements$),
        map(([defaultSidebar, customSidebar, ]) => [...defaultSidebar, ...customSidebar])
      )
  }
}
