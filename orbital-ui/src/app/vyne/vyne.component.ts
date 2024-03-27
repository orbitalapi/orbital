import {Component, OnInit} from '@angular/core';
import {BreakpointObserver} from '@angular/cdk/layout';
import {BehaviorSubject, combineLatest, Observable} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {AppInfo, AppInfoService} from '../services/app-info.service';
import {NavigationEnd, RouteConfigLoadEnd, RouteConfigLoadStart, Router} from '@angular/router';
import {SchemaNotificationService, SourceNameWithPackage} from '../services/schema-notification.service';
import {MatLegacySnackBar as MatSnackBar} from '@angular/material/legacy-snack-bar';
import {SystemAlert} from '../system-alert/system-alert.component';
import {TypesService} from '../services/types.service';
import {UserInfoService, VynePrivileges, VyneUser} from '../services/user-info.service';
import {DatePipe} from '@angular/common';
import {UiCustomisations} from "../../environments/ui-customisations";
import {PackageIdentifier, PackagesService} from "../package-viewer/packages.service";

@Component({
  selector: 'vyne-app',
  templateUrl: './vyne.component.html',
  styleUrls: ['./vyne.component.scss']
})
export class VyneComponent implements OnInit {
  sidebarElements: SidebarElement[] = [
    {
      title: 'Catalog',
      icon: 'assets/img/tabler/book2.svg',
      // icon: 'explore',
      // icon: 'outline-explore.svg',
      // iconActive: 'outline-explore-active.svg',
      route: 'catalog',
      testId: 'data-catalog-sidebar',
      requiredAuthority: VynePrivileges.BrowseCatalog
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
      title: 'Designer',
      icon: 'assets/img/tabler/tools.svg',
      route: 'designer',
      testId: 'designer',
      requiredAuthority: VynePrivileges.BrowseSchema
    },
    {
      title: 'Endpoints',
      icon: 'assets/img/tabler/traffic-lights.svg',
      route: 'endpoints',
      testId: 'endpoints',
      requiredAuthority: VynePrivileges.BrowseSchema
    },
    // {
    //   title: 'Pipeline manager',
    //   icon: 'assets/img/pipeline.svg',
    //   route: 'pipeline-manager',
    //   testId: 'pipeline-sidebar',
    //   requiredAuthority: VynePrivileges.ViewPipelines
    // },

  ].map(value => {
    return {
      title: value.title,
      icon: value.icon,
      iconActive: value.icon,
      route: value.route,
      testId: value.testId,
      requiredAuthority: value.requiredAuthority
    };
  });

  customSidebarElements$: BehaviorSubject<SidebarElement[]> = new BehaviorSubject([]);
  defaultSidebarElements$: BehaviorSubject<SidebarElement[]> = new BehaviorSubject<SidebarElement[]>(this.sidebarElements)
  sidebarElements$: Observable<SidebarElement[]> = combineLatest(
    [
      this.defaultSidebarElements$,
      this.customSidebarElements$,
    ]
  ).pipe(
    map(([arr1, arr2]) => [...arr1, ...arr2])
  )

  appInfo: AppInfo;
  userInfo: VyneUser | null = null;
  alerts: SystemAlert[] = [];

  isLoadingRoute$: Observable<boolean>;

  constructor(private breakpointObserver: BreakpointObserver,
              private appInfoService: AppInfoService,
              private router: Router,
              private schemaNotificationService: SchemaNotificationService,
              private typeService: TypesService,
              private snackbar: MatSnackBar,
              private userInfoService: UserInfoService,
              private datePipe: DatePipe,
              private packagesService: PackagesService,
  ) {
    appInfoService
      .getConfig()
      .subscribe(config => {
        if (!config.licenseStatus.isLicensed) {
          // this.setUnlicensedAlert(config.licenseStatus.expiresOn);
        }
        appInfoService
          .getAppInfo(config.actuatorPath)
          .subscribe(info => this.appInfo = info)

        this.customSidebarElements$.next(UiCustomisations.customSidebarElements(config))
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

    // Only show snackbar updates after we've loaded the initial state-of-the-world
    let isFirstSchemaUpdate = true;

    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(schemaUpdateNotification => {
        let message: string;
        if (schemaUpdateNotification.sourceNamesWithErrors.length > 0) {
          message = 'Schema has been updated, but contains compilation errors';
          this.setCompilationErrorAlert(schemaUpdateNotification.sourceNamesWithErrors[0]);
        } else {
          message = 'Schema has been updated';
          const alertIndex = this.getCompilationErrorsAlertIndex();
          if (alertIndex >= 0) {
            this.alerts.splice(alertIndex, 1);
          }
        }
        if (!isFirstSchemaUpdate) {
          this.snackbar.open(
            message, 'Dismiss', {
              duration: 5000,
            }
          );
        }
        isFirstSchemaUpdate = false;

      });

    this.userInfoService
      .userInfo$
      .pipe(
        filter(userInfo => userInfo != null),
        map(userInfo => this.sidebarElements
          .filter(sideBarElement => userInfo.grantedAuthorities.includes(sideBarElement.requiredAuthority))
        )
      ).subscribe(filteredSideBarElements => this.defaultSidebarElements$.next(filteredSideBarElements));

    this.packagesService.loadProjectLoadersWithErrors()
      .subscribe(projectsWithErrors => {
        if (projectsWithErrors.length > 0) {
          this.alerts.push({
            id: 'project-config-errors',
            severity: "Error",
            message: `${projectsWithErrors.length} of your projects has a configuration problem`,
            actionLabel: 'See details',
            handler: () => {
              this.router.navigate(['projects', 'problems'])
            }
          })
        }
      })

    this.packagesService.loadWorkspaceConfigStatus()
      .subscribe(status => {
        if (status.state === "ERROR") {
          this.alerts.push({
            id: 'project-config-errors',
            severity: "Error",
            message: `Your workspace cannot be loaded: ${status.message}`,
            actionLabel: 'Read docs',
            handler: () => {
              window.open('https://orbitalhq.com/docs/workspace/overview#workspace-conf-file', '_blank');
            }
          })
        }
      })
  }

  private getCompilationErrorsAlertIndex() {
    return this.alerts.findIndex(alert => alert.id === 'compilationErrors');
  }

  private setUnlicensedAlert(expirationDate: Date) {
    this.alerts.push({
      id: 'unlicensed',
      actionLabel: 'Request a free license',
      message: `No license detected, so using a temporary Enterprise license.  Vyne will shut down at ${this.datePipe.transform(expirationDate, 'shortTime')}`,
      severity: 'Info',
      handler: () => {
        window.open('https://join.slack.com/t/vyne-dev/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg');
      }
    })
  }

  private setCompilationErrorAlert(srcToNavigateTo: SourceNameWithPackage) {
    if (this.getCompilationErrorsAlertIndex() !== -1) {
      return;
    }
    // /projects/demo.vyne:films-demo:0.1.0/source?selectedFile=src%2Fstaff%2Ftypes%2FStaffId.taxi
    this.alerts.push({
      id: 'compilationErrors',
      actionLabel: 'View sources',
      message: 'Compilation errors detected',
      severity: 'Warning',
      handler: () => {
        this.router.navigate(['projects', srcToNavigateTo.packageIdentifier.uriSafeId, 'source'], {
          queryParams: {
            selectedFile: srcToNavigateTo.name
          }
        });
      }
    });
  }

  ngOnInit(): void {
    // this.typeService.getSchemaSummary()
    //   .subscribe(summary => {
    //     if (summary.invalidSourceCount > 0) {
    //       this.setCompilationErrorAlert(summary.sourceNamesWithErrors[0]);
    //     }
    //   });
    this.isLoadingRoute$ = this.router.events
      .pipe(
        filter(event => event instanceof RouteConfigLoadStart || event instanceof RouteConfigLoadEnd),
        map(event => event instanceof RouteConfigLoadStart)
      )
  }
}


export interface SidebarElement {
  title: string;
  icon: string;
  iconActive: string;
  route?: string;
  requiredAuthority?: VynePrivileges;
  externalUrl?: string;
}
