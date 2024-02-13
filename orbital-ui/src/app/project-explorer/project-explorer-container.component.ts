import { Component, Directive } from '@angular/core';
import { AppInfoService, AppConfig } from '../services/app-info.service';
import { ActivatedRoute, Router } from '@angular/router';
import { PackagesService, SourcePackageDescription } from 'src/app/package-viewer/packages.service';
import { Observable } from 'rxjs/internal/Observable';
import { SchemaNotificationService } from 'src/app/services/schema-notification.service';

@Directive()
export class BaseProjectExplorerContainer {
  config: AppConfig;
  packages: Observable<SourcePackageDescription[]>;

  constructor(private configService: AppInfoService,
              private router: Router,
              private activatedRoute: ActivatedRoute,
              private packagesService: PackagesService,
              private schemaNotificationService: SchemaNotificationService
  ) {
    this.loadProjects();
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(() => {
        this.loadProjects();
      });
    this.configService.getConfig().subscribe(result => this.config = result);
  }

  navigateToPackage(sourcePackage: SourcePackageDescription) {
    this.router.navigate([sourcePackage.uriPath], { relativeTo: this.activatedRoute })
  }

  private loadProjects() {
    this.packages = this.packagesService.listPackages()
  }
}


@Component({
  selector: 'app-project-explorer-container',
  template: `
      <app-panel-header title="Projects">
          <div class="spacer"></div>
          <button tuiButton size="s" appearance="outline" class='button-small menu-bar-button' [routerLink]="['/project-import']">Add a new Project

          </button>
      </app-panel-header>
      <div class="container">
          <app-package-list [packages]="packages | async"
                            (packageClicked)="navigateToPackage($event)"></app-package-list>
          <router-outlet></router-outlet>
      </div>

  `,
  styleUrls: ['./project-explorer-container.component.scss']
})
export class ProjectExplorerContainerComponent extends BaseProjectExplorerContainer {


}

