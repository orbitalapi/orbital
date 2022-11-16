import { Component, OnInit } from '@angular/core';
import { AppInfoService, QueryServiceConfig } from '../services/app-info.service';
import { ActivatedRoute, Router } from '@angular/router';
import { PackagesService, SourcePackageDescription } from 'src/app/package-viewer/packages.service';
import { Observable } from 'rxjs/internal/Observable';
import { SchemaNotificationService } from 'src/app/services/schema-notification.service';

@Component({
  selector: 'app-schema-explorer-container',
  template: `
    <app-header-bar title="Schema Explorer">
      <div class="add-new" *ngIf="config?.server.newSchemaSubmissionEnabled">
        <button mat-stroked-button [routerLink]="['/schema-importer']">Add new</button>
        <!--<mat-icon svgIcon="add_box"></mat-icon>-->
        <!--Add a new one-->
        <mat-menu #appMenu="matMenu">
          <button mat-menu-item (click)="importSchemaFromUrl()">Add schema from url</button>
          <!--<button mat-menu-item (click)="createNewSchema()">Add schema directly</button>-->
        </mat-menu>
      </div>
      <app-changeset-selector></app-changeset-selector>
    </app-header-bar>
    <div class="container">
      <app-package-list [packages]="packages | async" (packageClicked)="navigateToPackage($event)"></app-package-list>
      <router-outlet></router-outlet>
    </div>

  `,
  styleUrls: ['./schema-explorer-container.component.scss']
})
export class SchemaExplorerContainerComponent {

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

  importSchemaFromUrl() {
    this.router.navigate(['schema-importer']);
  }

  navigateToPackage(sourcePackage: SourcePackageDescription) {
    this.router.navigate([sourcePackage.uriPath], { relativeTo: this.activatedRoute })
  }

  private loadSchemas() {
    this.packages = this.packagesService.listPackages()
  }
}
