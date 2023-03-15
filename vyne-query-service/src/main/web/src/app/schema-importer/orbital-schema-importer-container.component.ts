import { Component } from '@angular/core';

@Component({
  selector: 'app-orbital-schema-importer-container',
  template: `
    <app-header-bar>
    </app-header-bar>
    <app-header-component-layout title="Add new schemas" [padBottom]="false">
      <ng-container ngProjectAs="header-components">
        <tui-tabs [(activeItemIndex)]="activeTabIndex">
          <!--          <button tuiTab>Push from application</button>-->
          <!--          <button tuiTab>CI Pipeline</button>-->
          <button tuiTab>Git Repository</button>
          <button tuiTab>Local disk</button>
          <button tuiTab>Import</button>
        </tui-tabs>
      </ng-container>

      <!--      <app-push-schema-config-panel-->
      <!--        *ngIf="activeTabIndex===0"-->
      <!--      ></app-push-schema-config-panel>-->
      <app-git-config *ngIf="activeTabIndex===0"></app-git-config>
      <app-file-config *ngIf="activeTabIndex===1"></app-file-config>
      <app-schema-importer title="" *ngIf="activeTabIndex===2"></app-schema-importer>

    </app-header-component-layout>
  `,
  styleUrls: ['./orbital-schema-importer-container.component.scss']
})
export class OrbitalSchemaImporterContainerComponent {
  activeTabIndex: number = 0;
}
