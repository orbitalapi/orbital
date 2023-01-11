import { Component, OnInit } from '@angular/core';
import { BaseSchemaExplorerContainer } from 'src/app/schema-explorer/schema-explorer-container.component';

@Component({
  selector: 'app-orbital-schema-explorer-container',
  template: `
    <div class="bg-white grow flex flex-col h-full">
      <div class="panel-toolbar">
        <span class="h3 flex-none">Schemas</span>
        <span class="flex-1"></span>
        <button tuiButton type="button" appearance="outline" size="m" routerLink="/schema-importer">Add new schemas</button>
      </div>
      <div class="bg-white grow flex container">
        <app-package-list [packages]="packages | async" (packageClicked)="navigateToPackage($event)"></app-package-list>
        <router-outlet></router-outlet>
      </div>
    </div>

  `,
  styleUrls: ['./orbital-schema-explorer-container.component.scss']
})
export class OrbitalSchemaExplorerContainerComponent extends BaseSchemaExplorerContainer {


}
