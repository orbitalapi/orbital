import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, Type} from '../services/schema';
import {CsvOptions, XmlIngestionParameters} from '../services/types.service';
import {FileSystemEntry} from 'ngx-file-drop';
import {NgxFileDropEntry} from 'ngx-file-drop/ngx-file-drop/ngx-file-drop-entry';

@Component({
  selector: 'app-data-source-toolbar',
  template: `
    <div class="container mat-elevation-z2">
      <div class="upload-container" *ngIf="!fileDataSource">
        <app-data-source-upload (fileSelected)="onFileSelected($event)"></app-data-source-upload>
      </div>
      <div class="data-source-configuration" *ngIf="fileDataSource">
        <app-data-source-config [fileDataSource]="fileDataSource"
                                (csvOptionsChanged)="csvOptionsChanged.emit($event)"
                                (xmlOptionsChanged)="xmlOptionsChanged.emit($event)"
                                (clear)="clearSelectedFile()"></app-data-source-config>
        <span>as</span>
        <app-type-autocomplete placeholder="Select type to apply to content" [schema]="schema"
                               (selectedTypeChange)="selectedTypeChanged.emit($event)"
        ></app-type-autocomplete>
        <button (click)=showTypeNamePanel($event) mat-stroked-button style="margin-left: 2rem">New...</button>
      </div>
    </div>
  `,
  styleUrls: ['./data-source-toolbar.component.scss']
})
export class DataSourceToolbarComponent {

  @Input()
  schema: Schema;

  @Input()
  fileDataSource: NgxFileDropEntry;

  @Output()
  cleared = new EventEmitter<void>();

  @Output()
  fileDataSourceChanged = new EventEmitter<NgxFileDropEntry>();

  @Output()
  selectedTypeChanged = new EventEmitter<Type>();

  @Output()
  csvOptionsChanged = new EventEmitter<CsvOptions>();

  @Output() isNewTypeClicked = new EventEmitter<boolean>();

  @Output()
  xmlOptionsChanged = new EventEmitter<XmlIngestionParameters>();

  clearSelectedFile() {
    this.fileDataSource = null;
    this.cleared.emit();
  }

  showTypeNamePanel(event) {
    this.isNewTypeClicked.emit(true);
  }

  onFileSelected(event: NgxFileDropEntry) {
    this.fileDataSource = event;
    this.fileDataSourceChanged.emit(this.fileDataSource);
  }
}
