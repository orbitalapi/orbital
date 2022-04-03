import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {NgxFileDropEntry} from 'ngx-file-drop';
import {readSingleFile} from '../../../utils/files';
import {ConvertSchemaEvent, ProtobufSchemaConverterOptions} from '../../schema-importer.models';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-protobuf-config',
  styleUrls: ['./protobuf-config.component.scss'],
  template: `
    <div class="form-container">
      <div class="form-body">
        <div class="form-row">
          <div class="form-item-description-container">
            <h3>Protobuf source</h3>
            <div class="help-text">
              Select where to load the protobuf from - either upload the schema directly, or specify a url
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
                <app-data-source-upload promptText="Drop your Protobuf file here"
                                        (fileSelected)="handleSchemaFileDropped($event)"></app-data-source-upload>
              </div>
              <div *ngSwitchCase="1" class="tab-panel">
                <tui-input [(ngModel)]="protobufSchemaConverterOptions.url"
                           (ngModelChange)="handleUrlUpdated($event)">
                  Protobuf Url
                </tui-input>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="form-button-bar">
      <button tuiButton [showLoader]="working" [size]="'m'" (click)="doCreate()">Create
      </button>
    </div>`
})
export class ProtobufConfigComponent {

  activeTabIndex: number = 0;
  protobufSchemaConverterOptions = new ProtobufSchemaConverterOptions();

  @Input()
  working: boolean = false;

  @Output()
  loadSchema = new EventEmitter<ConvertSchemaEvent>()

  handleSchemaFileDropped($event: NgxFileDropEntry) {
    this.protobufSchemaConverterOptions.url = null;
  }

  handleUrlUpdated($event: any) {
    this.protobufSchemaConverterOptions.protobuf = null;
  }

  doCreate() {
    console.log(JSON.stringify(this.protobufSchemaConverterOptions, null, 2));
    this.loadSchema.next(new ConvertSchemaEvent('protobuf', this.protobufSchemaConverterOptions));
  }
}
