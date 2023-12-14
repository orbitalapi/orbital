import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema} from '../services/schema';
import {ParsedCsvContent} from '../services/types.service';
import {FileSourceChangedEvent} from './data-source-panel/data-source-panel.component';

@Component({
  selector: 'app-source-mode-selector',
  template: `
    <div class="selector" *ngIf="mode !== 'table'">
      <div class="table-selector mat-elevation-z1" *ngIf="mode === 'unknown'">
        <button mat-stroked-button (click)="this.mode = 'table'">Edit in a table</button>
      </div>
      <app-data-source-panel
        *ngIf="mode === 'file' || mode === 'unknown'"
        class="component"
        [schema]="schema"
        (fileDataSourceChanged)="onFileDataSourceChanged($event)"
        [parsedCsvContent]="parsedCsvContent"
      ></app-data-source-panel>
    </div>
    <app-table-editor *ngIf="mode === 'table'" (csvDataUpdated)="csvDataUpdated.emit($event)"></app-table-editor>
  `,
  styleUrls: ['./source-mode-selector.component.scss']
})
export class SourceModeSelectorComponent {

  mode: 'table' | 'file' | 'unknown' = 'unknown';
  @Input()
  schema: Schema;
  @Input()
  parsedCsvContent: ParsedCsvContent;

  @Output()
  fileDataSourceChanged = new EventEmitter<FileSourceChangedEvent>();

  @Output()
  csvDataUpdated = new EventEmitter<string>();

  onFileDataSourceChanged($event: FileSourceChangedEvent) {
    this.mode = 'file';
    this.fileDataSourceChanged.emit($event);
  }
}
