import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema} from '../../services/schema';
import {CsvOptions, ParsedCsvContent, XmlIngestionParameters} from '../../services/types.service';
import {UploadFile} from 'ngx-file-drop';

@Component({
  selector: 'app-data-source-display',
  template: `
    <div class="csv-options-toolbar">
      <app-data-source-config [fileDataSource]="fileDataSource"
                              (csvOptionsChanged)="csvOptionsChange.emit($event)"
                              (xmlOptionsChanged)="xmlOptionsChanged.emit($event)"
      ></app-data-source-config>
      <div class="spacer"></div>
      <mat-button-toggle-group #group="matButtonToggleGroup" [value]="displayMode">
        <mat-button-toggle value="table" aria-label="Text align left">
          <mat-icon>table_chart</mat-icon>
        </mat-button-toggle>
        <mat-button-toggle value="source" aria-label="Text align center">
          <mat-icon>code</mat-icon>
        </mat-button-toggle>
      </mat-button-toggle-group>
    </div>
    <app-csv-viewer *ngIf="displayMode === 'table' && csvContents"
                    [schema]="schema"
                    [source]="csvContents"
                    [csvOptions]="csvOptions"
    >

    </app-csv-viewer>
  `,
  styleUrls: ['./data-source-display.component.scss']
})
export class DataSourceDisplayComponent {

  displayMode: 'table' | 'source' = 'table';

  @Input()
  schema: Schema;
  @Input()
  csvContents: ParsedCsvContent;
  @Input()
  fileContents: string;
  @Input()
  fileExtension: string;
  @Input()
  fileDataSource: UploadFile;
  @Input()
  csvOptions: CsvOptions;

  @Output()
  csvOptionsChange = new EventEmitter<CsvOptions>();

  @Output()
  xmlOptionsChanged = new EventEmitter<XmlIngestionParameters>();
}
