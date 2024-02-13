import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TuiTabsModule } from '@taiga-ui/kit';
import { HeaderComponentLayoutModule } from '../header-component-layout/header-component-layout.module';
import { ProjectSourceConfigModule } from './project-source-config/project-source-config.module';
import { DataSourceImportComponent } from '../data-source-import/data-source-import.component';

@Component({
  selector: 'app-project-import',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponentLayoutModule,
    TuiTabsModule,
    ProjectSourceConfigModule,
    DataSourceImportComponent
  ],
  template: `
    <app-header-component-layout title="Add a Project" [padBottom]="false" [fullWidth]="activeTabIndex===3">
      <ng-container ngProjectAs="header-components">
        <tui-tabs [(activeItemIndex)]="activeTabIndex">
          <!--          <button tuiTab>Push from application</button>-->
          <!--          <button tuiTab>CI Pipeline</button>-->
          <button tuiTab>Git Repository</button>
          <button tuiTab>Local disk</button>
          <button tuiTab>Add Data source</button>
        </tui-tabs>
      </ng-container>

      <!--      <app-push-schema-config-panel-->
      <!--        *ngIf="activeTabIndex===0"-->
      <!--      ></app-push-schema-config-panel>-->
      <app-git-config *ngIf="activeTabIndex===0"></app-git-config>
      <app-file-config *ngIf="activeTabIndex===1"></app-file-config>
      <app-data-source-import title="" *ngIf="activeTabIndex===2"></app-data-source-import>

    </app-header-component-layout>
  `,
  styleUrls: ['./project-import.component.scss']
})
export class ProjectImportComponent {
  activeTabIndex: number = 0;
}
