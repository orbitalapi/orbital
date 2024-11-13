import { TuiInputModule } from "@taiga-ui/legacy";
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgxFileDropEntry } from 'ngx-file-drop';
import { tuiNotificationOptionsProvider, TuiNotification, TuiButton, TuiHint } from '@taiga-ui/core';
import { TuiTabs, TuiButtonLoading } from '@taiga-ui/kit';
import {UiCustomisations} from '../../../../environments/ui-customisations';
import { ConvertSchemaEvent, SwaggerConverterOptions } from '../../data-source-import.models';
import { readSingleFile } from '../../../utils/files';
import { PackageIdentifier } from '../../../package-viewer/packages.service';
import { DataExplorerModule } from '../../../data-explorer/data-explorer.module';
import { sanitiseNamespace } from '../../../utils/utils';

@Component({
  selector: 'app-swagger-config',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TuiButton,
    TuiInputModule,
    TuiTabs,
    DataExplorerModule,
    TuiNotification,
    TuiHint,
    TuiButtonLoading
],
  providers: [
    tuiNotificationOptionsProvider({
      icon: '@tui.circle-help',
      appearance: 'info',
    }),
  ],
  template: `
    <div class="form-container">
      <form class="form-body" #swaggerForm="ngForm">
        <tui-notification appearance="info" class="open-api-notification" [tuiHint]="tooltip" tuiHintAppearance="onDark" tuiHintShowDelay="100">
          Understanding the difference between adding a project vs adding a data source
        </tui-notification>
        <ng-template #tooltip>
          <p>Using this approach, the OpenAPI spec is converted to Taxi, and stored in your project, and the Taxi version becomes the source of truth.</p>
          <p>Use this when you'd prefer to work with Taxi over OpenAPI.</p>
          <p>Alternatively, you can add the spec directly as a project, reading from disk or a git repository. This works well when you plan on keeping your OAS spec as a source of truth, and can embed Taxi metadata directly inside it.</p>
        </ng-template>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>OpenAPI spec source</h3>
            <div class="help-text">
              Select where to load the OpenAPI spec from - either upload the schema directly, or specify a url
            </div>
          </div>
          <div class="form-element">
            <tui-tabs [(activeItemIndex)]="activeTabIndex">
              <button tuiTab>
                <img src="assets/img/tabler/file-code.svg" class="icon">
                File
              </button>
              <button tuiTab>
                <img src="assets/img/url.svg" class="icon">
                Url
              </button>
            </tui-tabs>
            <div [ngSwitch]="activeTabIndex">
              <div *ngSwitchCase="0" class="tab-panel">
                <app-data-source-upload promptText="Drop your OpenAPI schema file here"
                                        (fileSelected)="handleSchemaFileDropped($event)"></app-data-source-upload>
              </div>
              <div *ngSwitchCase="1" class="tab-panel">
                <tui-input [(ngModel)]="swaggerOptions.url" (ngModelChange)="swaggerOptions.swagger = null;" name="url" required>
                  Swagger / OpenAPI URL
                </tui-input>
              </div>
            </div>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Default namespace</h3>
            <div class="help-text">
              Defines a namespace which any newly created types will be created in
            </div>
          </div>
          <div class="form-element">
            <tui-input [(ngModel)]="swaggerOptions.defaultNamespace" name="defaultNamespace" required>
              Default namespace
            </tui-input>
          </div>
        </div>
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Base Url</h3>
            <div class="help-text">
              Define a base url, which services are relative to. Only required if the OpenAPI spec doesn't define this
              itself
            </div>
          </div>
          <div class="form-element">
            <tui-input [(ngModel)]="swaggerOptions.serviceBasePath">
              Base Url
            </tui-input>
          </div>
        </div>
        <input hidden [ngModel]="(swaggerOptions.swagger || swaggerOptions.url) ? 1 : ''" required name="hiddenField"/>
      </form>
    </div>
    <div class="form-button-bar">
      <button tuiButton [loading]="working" [size]="'m'" (click)="doCreate()" [disabled]="swaggerForm.invalid">Configure
      </button>
    </div>
  `,
  styleUrls: ['./swagger-config.component.scss']
})
export class SwaggerConfigComponent {

  activeTabIndex: number = 0;

  swaggerOptions = new SwaggerConverterOptions();

  @Input()
  working: boolean = false;

  @Output()
  loadSchema = new EventEmitter<ConvertSchemaEvent>()

  @Input()
  packageIdentifier: PackageIdentifier;

  doCreate() {
    console.log(JSON.stringify(this.swaggerOptions, null, 2));
    this.loadSchema.next(new ConvertSchemaEvent('swagger', this.swaggerOptions, this.packageIdentifier));
  }

  handleSchemaFileDropped($event: NgxFileDropEntry) {
    this.swaggerOptions.url = null;
    readSingleFile($event)
      .subscribe((text: string) => {
        this.swaggerOptions.swagger = text;
        const { organisation, name } = this.packageIdentifier;
        this.swaggerOptions.defaultNamespace = sanitiseNamespace(`${organisation}.${name}`);
      })
  }

  protected readonly uiConfig = UiCustomisations;
}
