import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ConvertSchemaEvent, JsonSchemaConverterOptions, JsonSchemaVersion} from '../../schema-importer.models';
import {NgxFileDropEntry} from 'ngx-file-drop';
import {readSingleFile} from '../../../utils/files';
import {Observable} from 'rxjs/internal/Observable';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-jsonschema-config',
  template: `
    <div class="form-container">
      <div class="form-body">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>JsonSchema source</h3>
            <div class="help-text">
              Select where to load the JsonSchema from - either upload the schema directly, or specify a url
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
                <app-data-source-upload promptText="Drop your JsonSchema file here"
                                        (fileSelected)="handleSchemaFileDropped($event)"></app-data-source-upload>
              </div>
              <div *ngSwitchCase="1" class="tab-panel">
                <tui-input [(ngModel)]="jsonSchemaConverterOptions.url"
                           (ngModelChange)="handleUrlUpdated($event)">
                  JsonSchema Url
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
            <tui-input [(ngModel)]="jsonSchemaConverterOptions.defaultNamespace">
              Default namespace
            </tui-input>
          </div>
        </div>
      </div>
      <div class="form-row" *ngIf="jsonSchemaConverterOptions.url">
        <div class="form-item-description-container">
          <h3>Base Url</h3>
          <div class="help-text">
            If the JsonSchema contains relative links for models, then specify the url that links should be resolved
            against.
          </div>
        </div>
        <div class="form-element">
          <tui-input [(ngModel)]="jsonSchemaConverterOptions.resolveUrlsRelativeToUrl">
            Base Url
          </tui-input>
        </div>
      </div>
      <div class="form-row">
        <div class="form-item-description-container">
          <h3>JsonSchema version</h3>
          <div class="help-text">
            Set the version of the JsonSchema spec that this schema uses. If you're not sure, it's ok to leave this as
            INFERRED.
          </div>
        </div>
        <div class="form-element">
          <tui-select [(ngModel)]="jsonSchemaConverterOptions.schemaVersion">
            Select the JsonSchema version
            <tui-data-list-wrapper
              *tuiDataList
              [items]="jsonSchemaVersions | tuiFilterByInputWith : stringify"
              [itemContent]="stringify | tuiStringifyContent"
            ></tui-data-list-wrapper>
          </tui-select>
        </div>
      </div>
    </div>

    <div class="form-button-bar">
      <button tuiButton [showLoader]="working" [size]="'m'" (click)="doCreate()">Create
      </button>
    </div>
  `,
  styleUrls: ['./jsonschema-config.component.scss']
})
export class JsonSchemaConfigComponent {

  jsonSchemaVersions: JsonSchemaVersionOption[] = [
    {label: 'Inferred', value: 'INFERRED'},
    {label: 'Draft 6', value: 'DRAFT_6'},
    {label: 'Draft 7', value: 'DRAFT_7'},
  ];

  activeTabIndex: number = 0;

  jsonSchemaConverterOptions = new JsonSchemaConverterOptions();

  @Output()
  loadSchema = new EventEmitter<ConvertSchemaEvent>()

  @Input()
  working: boolean = false;

  doCreate() {
    console.log(JSON.stringify(this.jsonSchemaConverterOptions, null, 2));
    this.loadSchema.next(new ConvertSchemaEvent('jsonSchema', this.jsonSchemaConverterOptions));
  }

  handleSchemaFileDropped($event: NgxFileDropEntry) {
    this.jsonSchemaConverterOptions.url = null;
    readSingleFile($event)
      .subscribe((text: string) => {
        this.jsonSchemaConverterOptions.jsonSchema = text;
      });
  }

  readonly stringify = (item: JsonSchemaVersionOption) => item.label;

  handleUrlUpdated($event: any) {
    this.jsonSchemaConverterOptions.jsonSchema = null;
    if (isNullOrUndefined(this.jsonSchemaConverterOptions.resolveUrlsRelativeToUrl)) {
      this.jsonSchemaConverterOptions.resolveUrlsRelativeToUrl = this.deriveBaseUrl(this.jsonSchemaConverterOptions.url);
    }
  }

  private deriveBaseUrl(url: string): string {
    const urlParts = url.split('/');
    // remove the last item - likely the name of the document
    urlParts.pop();
    return urlParts.join('/') + '/';
  }
}

export interface JsonSchemaVersionOption {
  label: string;
  value: JsonSchemaVersion
}
