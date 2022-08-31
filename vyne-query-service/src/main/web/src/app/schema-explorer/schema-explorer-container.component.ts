import { Component, OnInit } from '@angular/core';
import { AppInfoService, QueryServiceConfig } from '../services/app-info.service';
import { Router } from '@angular/router';

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
    </app-header-bar>
    <router-outlet></router-outlet>
  `,
  styleUrls: ['./schema-explorer-container.component.scss']
})
export class SchemaExplorerContainerComponent {

  config: QueryServiceConfig;

  constructor(private configService: AppInfoService, private router: Router,) {
    this.configService.getConfig().subscribe(result => this.config = result);
  }

  importSchemaFromUrl() {
    this.router.navigate(['schema-importer']);
  }
}
