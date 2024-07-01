import { Component, Directive } from '@angular/core';
import { AppInfoService, AppConfig } from '../services/app-info.service';
import { ActivatedRoute, Router } from '@angular/router';
import {
  PackageIdentifier,
  PackagesService,
  ProjectLoaderWithStatus,
  SourcePackageDescription
} from 'src/app/package-viewer/packages.service';
import { Observable } from 'rxjs/internal/Observable';
import {SchemaNotificationService, SchemaUpdatedNotification} from 'src/app/services/schema-notification.service';
import {TypesService} from "../services/types.service";

@Directive()
export class BaseProjectExplorerContainer {
  config: AppConfig;
  packages: Observable<SourcePackageDescription[]>;
  unhealthyLoaders$: Observable<ProjectLoaderWithStatus[]>;

  packageIdsWithErrors: string[] = [];

  constructor(private configService: AppInfoService,
              private router: Router,
              private activatedRoute: ActivatedRoute,
              private packagesService: PackagesService,
              private typesService: TypesService,
              private schemaNotificationService: SchemaNotificationService
  ) {
    this.loadProjects();
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe((next) => {
        this.updatePackagesWithCompilationErrors(next)
        this.loadProjects();
      });
    this.configService.getConfig().subscribe(result => this.config = result);
  }

  private loadProjects() {
    this.packages = this.packagesService.listPackages()
    this.unhealthyLoaders$ = this.packagesService.loadProjectLoadersWithErrors()
  }

  showProjectsWithProblems() {
    this.router.navigate(['problems'], { relativeTo: this.activatedRoute })
  }

  private updatePackagesWithCompilationErrors(update: SchemaUpdatedNotification) {
    // Use a map to only get the unique packageIdentifiers
    const packagesWithErrors = new Map<string, PackageIdentifier>()
    update.sourceNamesWithErrors.forEach(sourceName => {
      packagesWithErrors.set(sourceName.packageIdentifier.id, sourceName.packageIdentifier)
    })
    this.packageIdsWithErrors = Array.from(packagesWithErrors.keys())
  }
}


@Component({
  selector: 'app-project-explorer-container',
  template: `
    <app-panel-header title="Projects">
      <div class="spacer"></div>
      <button
        *appRequiresAuthority="['EditSchema']"
        tuiButton
        size="s"
        appearance="primary"
        icon="tuiIconPlus"
        class='button-small menu-bar-button'
        [routerLink]="['project-import']"
      >
        Add project
      </button>
    </app-panel-header>
    <div class="container">
      <app-package-list [packages]="packages | async"
                        [projectsWithProblems]="unhealthyLoaders$ | async"
                        [packagesWithCompilationErrors]="packageIdsWithErrors"
                        (showProjectsWithProblems)="showProjectsWithProblems()"
      ></app-package-list>
      <router-outlet></router-outlet>
    </div>

  `,
  styleUrls: ['./project-explorer-container.component.scss']
})
export class ProjectExplorerContainerComponent extends BaseProjectExplorerContainer {


}

