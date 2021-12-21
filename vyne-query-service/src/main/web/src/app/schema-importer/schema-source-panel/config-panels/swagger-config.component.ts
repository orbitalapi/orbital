import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {SwaggerConverterOptions} from '../../schema-importer.models';
import {NgxFileDropEntry} from 'ngx-file-drop';
import {readSingleFile} from '../../../utils/files';

@Component({
  selector: 'app-swagger-config',
  template: `
    <div class="form-container">
      <div class="form-body">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Swagger source</h3>
            <div class="help-text">
              Select where to load the swagger from - either upload the schema directly, or specify a url
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
                <app-data-source-upload promptText="Drop your swagger schema file here"
                                        (fileSelected)="handleSchemaFileDropped($event)"></app-data-source-upload>
              </div>
              <div *ngSwitchCase="1" class="tab-panel">
                <tui-input [(ngModel)]="swaggerOptions.url" (ngModelChange)="swaggerOptions.swagger = null;">
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
            <tui-input [(ngModel)]="swaggerOptions.defaultNamespace">
              Default namespace
            </tui-input>
          </div>
        </div>
      </div>
      <div class="form-row">
        <div class="form-item-description-container">
          <h3>Base Url</h3>
          <div class="help-text">
            Define a base url, which services are relative to. Only required if the swagger spec doesn't define this
            itself
          </div>
        </div>
        <div class="form-element">
          <tui-input [(ngModel)]="swaggerOptions.serviceBasePath">
            Base Url
          </tui-input>
        </div>
      </div>
    </div>
    <div class="form-button-bar">
      <button mat-flat-button color="primary" (click)="doCreate()">Create
      </button>
    </div>
  `,
  styleUrls: ['./swagger-config.component.scss']
})
export class SwaggerConfigComponent {

  activeTabIndex: number = 0;

  swaggerOptions = new SwaggerConverterOptions();

  @Output()
  loadSchema = new EventEmitter<SwaggerConverterOptions>()

  doCreate() {
    console.log(JSON.stringify(this.swaggerOptions, null, 2));
    this.loadSchema.next(this.swaggerOptions);
  }

  handleSchemaFileDropped($event: NgxFileDropEntry) {
    this.swaggerOptions.url = null;
    readSingleFile($event)
      .subscribe((text: string) => {
        this.swaggerOptions.swagger = text;
      });
  }
}
