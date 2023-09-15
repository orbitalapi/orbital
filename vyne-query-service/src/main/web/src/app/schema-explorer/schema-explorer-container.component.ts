import { Component, Directive } from '@angular/core';
import { AppInfoService, QueryServiceConfig } from '../services/app-info.service';
import { ActivatedRoute, Router } from '@angular/router';
import { PackagesService, SourcePackageDescription } from 'src/app/package-viewer/packages.service';
import { Observable } from 'rxjs/internal/Observable';
import { SchemaNotificationService } from 'src/app/services/schema-notification.service';

@Directive()
export class BaseSchemaExplorerContainer {
  config: QueryServiceConfig;
  packages: Observable<SourcePackageDescription[]>;

  constructor(private configService: AppInfoService,
              private router: Router,
              private activatedRoute: ActivatedRoute,
              private packagesService: PackagesService,
              private schemaNotificationService: SchemaNotificationService
  ) {
    this.loadSchemas();
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(() => {
        this.loadSchemas();
      });
    this.configService.getConfig().subscribe(result => this.config = result);
  }

  navigateToPackage(sourcePackage: SourcePackageDescription) {
    this.router.navigate([sourcePackage.uriPath], { relativeTo: this.activatedRoute })
  }

  private loadSchemas() {
    this.packages = this.packagesService.listPackages()
  }
}


@Component({
  selector: 'app-schema-explorer-container',
  template: `
      <app-panel-header title="Schemas">
          <div class="spacer"></div>
          <button mat-flat-button class='button-small menu-bar-button' [routerLink]="['/schema-importer']">Add a new schema

          </button>
      </app-panel-header>
      <div class="container">
          <app-package-list [packages]="packages | async"
                            (packageClicked)="navigateToPackage($event)"></app-package-list>
          <router-outlet></router-outlet>
      </div>

  `,
  styleUrls: ['./schema-explorer-container.component.scss']
})
export class SchemaExplorerContainerComponent extends BaseSchemaExplorerContainer {


}

