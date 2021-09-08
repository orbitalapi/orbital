import {Component, OnInit} from '@angular/core';
import {BreakpointObserver, Breakpoints} from '@angular/cdk/layout';
import {Observable} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {AppInfo, AppInfoService} from '../services/app-info.service';
import {NavigationEnd, Router} from '@angular/router';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {SystemAlert} from '../system-alert/system-alert.component';
import {TypesService} from '../services/types.service';
import {VyneUser, UserInfoService} from '../services/user-info.service';

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
      route: 'catalog'
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
      title: 'Data explorer',
      icon: 'assets/img/data-explorer.svg',
      route: 'data-explorer'
    },
    {
      title: 'Query history',
      icon: 'assets/img/history.svg',
      route: 'query-history'
    },
    {
      title: 'Cask',
      icon: 'assets/img/cask.svg',
      route: 'cask-viewer'
    },
    {
      title: 'Authentication manager',
      icon: 'assets/img/security.svg',
      route: 'authentication-manager'
    },


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
  userInfo: VyneUser | null = null;
  alerts: SystemAlert[] = [];

  constructor(private breakpointObserver: BreakpointObserver,
              private appInfoService: AppInfoService,
              private router: Router,
              private schemaNotificationService: SchemaNotificationService,
              private typeService: TypesService,
              private snackbar: MatSnackBar) {
    appInfoService
      .getConfig()
      .subscribe(config =>
        appInfoService
          .getAppInfo(config.actuatorPath)
          .subscribe(info => this.appInfo = info));
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
  }

  private getAlertIndex() {
    return this.alerts.findIndex(alert => alert.id === 'compilationErrors');
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
}
