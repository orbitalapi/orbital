import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-orbital-schema-importer-container',
  template: `
    <div class="bg-white grow flex flex-col h-full">
      <div class="panel-toolbar">
        <span class="h3 flex-none">Add a new schema</span>
        <span class="flex-1"></span>
      </div>
      <div class="m4">
        <p class="text-lg">Choose how you'd like to publish your schemas</p>


        <tui-tabs [(activeItemIndex)]="activeTabIndex">
          <button tuiTab>Push</button>
          <button tuiTab>Git / File</button>
          <button tuiTab>Import</button>
        </tui-tabs>

        <app-push-schema-config-panel
          *ngIf="activeTabIndex===0"
        ></app-push-schema-config-panel>

        <app-schema-importer title="" *ngIf="activeTabIndex===2"></app-schema-importer>
      </div>
    </div>
  `,
  styleUrls: ['./orbital-schema-importer-container.component.scss']
})
export class OrbitalSchemaImporterContainerComponent {
  activeTabIndex: number = 0;
}
