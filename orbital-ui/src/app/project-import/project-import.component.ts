import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TuiStepperModule, TuiTabsModule } from '@taiga-ui/kit';
import { HeaderComponentLayoutModule } from '../header-component-layout/header-component-layout.module';
import { ProjectSourceConfigModule } from './project-source-config/project-source-config.module';
import { DataSourceImportComponent } from '../data-source-import/data-source-import.component';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-project-import',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponentLayoutModule,
    TuiTabsModule,
    ProjectSourceConfigModule,
    DataSourceImportComponent,
    RouterLink,
    RouterLinkActive,
    TuiStepperModule
  ],
  template: `
    <app-header-component-layout title="Add a Project" [padBottom]="false" [fullWidth]="overRideFullWidth">
      <ng-container ngProjectAs="header-components">
        <tui-tabs [(activeItemIndex)]="activeTabIndex">
          <!--          <button tuiTab>Push from application</button>-->
          <!--          <button tuiTab>CI Pipeline</button>-->
          <button tuiTab routerLink="/project-import/git-repository" routerLinkActive>Git Repository</button>
          <button tuiTab routerLink="/project-import/local-disk" routerLinkActive>Local disk</button>
          <button tuiTab routerLink="/project-import/data-source" routerLinkActive>Add Data source</button>
        </tui-tabs>
      </ng-container>

      <!--      <app-push-schema-config-panel-->
      <!--        *ngIf="activeTabIndex===0"-->
      <!--      ></app-push-schema-config-panel>-->
      <app-git-config *ngIf="activeTabIndex===0"></app-git-config>
      <app-file-config *ngIf="activeTabIndex===1"></app-file-config>
      <app-data-source-import title="" *ngIf="activeTabIndex===2" (onConfigureStep)="overRideFullWidth = $event"></app-data-source-import>

    </app-header-component-layout>
  `,
  styleUrls: ['./project-import.component.scss']
})
export class ProjectImportComponent {
  activeTabIndex: number = 0;
  overRideFullWidth: boolean;
}
