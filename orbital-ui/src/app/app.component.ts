import { DatePipe } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { NavigationEnd, RouteConfigLoadEnd, RouteConfigLoadStart, Router } from '@angular/router';
import { TuiAlertService } from '@taiga-ui/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { UiCustomisations } from '../environments/ui-customisations';
import { PackagesService } from './package-viewer/packages.service';
import { AppInfo, AppInfoService } from './services/app-info.service';
import { SchemaNotificationService, SourceNameWithPackage } from './services/schema-notification.service';
import { SidebarElement } from './sidenav/sidenav.component';
import { SystemAlert } from './system-alert/system-alert.component';
import {DbConnectionService} from "./db-connection-editor/db-importer.service";

@Component({
  selector: 'app-root',
  template: `
    <tui-root>
      <div class="content-wrapper">
        <app-sidenav [appInfo]="appInfo" [customSidebarElements$]="customSidebarElements$"></app-sidenav>
        <div class="app-header-and-content-container">
          <app-header-bar></app-header-bar>
          <progress
            max="100"
            tuiProgressBar
            size='xs'
            new
            *ngIf='isLoadingRoute$ | async'
          ></progress>
          <div class="alerts-container">
            <app-system-alert *ngFor="let alert of alerts" [alert]="alert"></app-system-alert>
          </div>
          <div class="app-page-content" [ngClass]="{ isLoading: isLoadingRoute$ | async}">
            <router-outlet></router-outlet>
            <app-draft-management-bar></app-draft-management-bar>
          </div>
        </div>
      </div>
    </tui-root>
  `,
  styleUrls: ['./app.component.scss'],
  providers: [AppInfoService]
})
export class AppComponent implements OnInit {
  appInfo: AppInfo;
  alerts: SystemAlert[] = [];
  customSidebarElements$: BehaviorSubject<SidebarElement[]> = new BehaviorSubject([]);
  isLoadingRoute$: Observable<boolean>;

  constructor(private appInfoService: AppInfoService,
              private router: Router,
              private schemaNotificationService: SchemaNotificationService,
              private datePipe: DatePipe,
              private packagesService: PackagesService,
              private dbService: DbConnectionService,
              @Inject(TuiAlertService) private readonly alertService: TuiAlertService,
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
          this.alertService.open(message, {status: this.alerts.length ? 'warning': 'success', autoClose: 5000 })
            .subscribe()
        }
        isFirstSchemaUpdate = false;

      });

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

    this.dbService.getConnections(false)
        .subscribe(connectionListResponse => {
          if (connectionListResponse.definitionsWithErrors.length > 0) {
            this.alerts.push({
              id: 'connection-config-errors',
              severity: "Error",
              message: `Your data sources cannot be loaded`,
              actionLabel: 'See details',
              handler: () => {
                this.router.navigate(["data-source-manager", "problems"]);
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
