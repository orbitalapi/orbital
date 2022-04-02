import {Component, OnInit} from '@angular/core';
import {BreakpointObserver, Breakpoints} from '@angular/cdk/layout';
import {BehaviorSubject, Observable} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {AppInfo, AppInfoService} from '../services/app-info.service';
import {NavigationEnd, Router} from '@angular/router';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {SystemAlert} from '../system-alert/system-alert.component';
import {TypesService} from '../services/types.service';
import {VyneUser, UserInfoService, VynePrivileges} from '../services/user-info.service';
import {DatePipe} from "@angular/common";

@Component({
  selector: 'vyne-app',
  templateUrl: './vyne.component.html',
  styleUrls: ['./vyne.component.scss']
})
export class VyneComponent implements OnInit {
  isHandset$: Observable<boolean> = this.breakpointObserver.observe(Breakpoints.Handset)
    .pipe(
      map(result => result.matches)
    );


  sidebarElements: SidebarElement[] = [
    {
      title: 'Data catalog',
      icon: 'assets/img/dictionary.svg',
      // icon: 'explore',
      // icon: 'outline-explore.svg',
      // iconActive: 'outline-explore-active.svg',
      route: 'catalog',
      testId: 'data-catalog-sidebar',
      requiredAuthority: VynePrivileges.BrowseCatalog
    },
    {
      title: 'Schema explorer',
      icon: 'assets/img/coding.svg',
      route: 'schema-explorer',
      testId: 'schema-explorer-sidebar',
      requiredAuthority: VynePrivileges.BrowseSchema
    },
    {
      title: 'Query builder',
      icon: 'assets/img/query.svg',
      // icon: 'outline-layers.svg',
      // iconActive: 'outline-layers-active.svg',
      route: 'query-wizard',
      testId: 'query-builder-sidebar',
      requiredAuthority: VynePrivileges.RunQuery
    },
    {
      title: 'Data explorer',
      icon: 'assets/img/data-explorer.svg',
      route: 'data-explorer',
      testId: 'data-explorer-sidebar',
      requiredAuthority: VynePrivileges.BrowseCatalog
    },
    {
      title: 'Query history',
      icon: 'assets/img/history.svg',
      route: 'query-history',
      testId: 'query-history-sidebar',
      requiredAuthority: VynePrivileges.ViewQueryHistory
    },
    {
      title: 'Cask',
      icon: 'assets/img/cask.svg',
      route: 'cask-viewer',
      testId: 'cask-sidebar',
      requiredAuthority: VynePrivileges.ViewCaskDefinitions
    },
    {
      title: 'Connection manager',
      icon: 'assets/img/connections.svg',
      route: 'connection-manager',
      requiredAuthority: VynePrivileges.ViewConnections
    },
    {
      title: 'Authentication manager',
      icon: 'assets/img/security.svg',
      route: 'authentication-manager',
      testId: 'authentication-sidebar',
      requiredAuthority: VynePrivileges.ViewAuthenticationTokens
    },
    {
      title: 'Pipeline manager',
      icon: 'assets/img/pipeline.svg',
      route: 'pipeline-manager',
      testId: 'pipeline-sidebar',
      requiredAuthority: VynePrivileges.ViewPipelines
    },

  ].map(value => {
    return {
      title: value.title,
      // icon: `assets/img/${value.icon}`,
      // iconActive: `assets/img/${value.icon}`,
      icon: value.icon,
      iconActive: value.icon,
      route: value.route,
      testId: value.testId,
      requiredAuthority: value.requiredAuthority
    };
  });

  sidebarElements$: BehaviorSubject<SidebarElement[]> = new BehaviorSubject(this.sidebarElements);

  appInfo: AppInfo;
  userInfo: VyneUser | null = null;
  alerts: SystemAlert[] = [];

  constructor(private breakpointObserver: BreakpointObserver,
              private appInfoService: AppInfoService,
              private router: Router,
              private schemaNotificationService: SchemaNotificationService,
              private typeService: TypesService,
              private snackbar: MatSnackBar,
              private userInfoService: UserInfoService,
              private datePipe: DatePipe) {
    appInfoService
      .getConfig()
      .subscribe(config => {
        if (!config.licenseStatus.isLicensed) {
          this.setUnlicensedAlert(config.licenseStatus.expiresOn);
        }
        appInfoService
          .getAppInfo(config.actuatorPath)
          .subscribe(info => this.appInfo = info)
      });
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
        document.querySelector('.app-page-content').scrollTop = 0;
      });

    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(schemaUpdateNotification => {
        let message: string;
        if (schemaUpdateNotification.invalidSourceCount > 0) {
          message = 'Schema has been updated, but contains compilation errors';
          this.setCompilationErrorAlert();
        } else {
          message = 'Schema has been updated';
          const alertIndex = this.getAlertIndex();
          if (alertIndex >= 0) {
            this.alerts.splice(alertIndex, 1);
          }
        }
        this.snackbar.open(
          message, 'Dismiss', {
            duration: 5000,
          }
        );
      });

    this.userInfoService
      .userInfo$
      .pipe(
        filter(userInfo => userInfo != null),
        map(userInfo => this.sidebarElements
          .filter(sideBarElement => userInfo.grantedAuthorities.includes(sideBarElement.requiredAuthority))
        )
      ).subscribe(filteredSideBarElements => this.sidebarElements$.next(filteredSideBarElements));
  }

  private getAlertIndex() {
    return this.alerts.findIndex(alert => alert.id === 'compilationErrors');
  }

  private setUnlicensedAlert(expirationDate: Date) {
    this.alerts.push({
      id: 'unlicensed',
      actionLabel: 'Contact us',
      message: `No license detected, so using a temporary Enterprise license.  Vyne will shut down at ${this.datePipe.transform(expirationDate, 'fullTime')}`,
      severity: 'Info',
      handler: () => {
        window.open('https://join.slack.com/t/vyne-dev/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg');
      }
    })
  }

  private setCompilationErrorAlert() {
    if (this.getAlertIndex() !== -1) {
      return;
    }
    this.alerts.push({
      id: 'compilationErrors',
      actionLabel: 'Go to schema explorer',
      message: 'Compilation errors detected in schemas',
      severity: 'Warning',
      handler: () => {
        this.router.navigate(['schema-explorer']);
      }
    });
  }

  ngOnInit(): void {
    this.typeService.getSchemaSummary()
      .subscribe(summary => {
        if (summary.invalidSourceCount > 0) {
          this.setCompilationErrorAlert();
        }
      });
  }
}


export interface SidebarElement {
  title: string;
  icon: string;
  iconActive: string;
  route: string;
  requiredAuthority: VynePrivileges;
}
