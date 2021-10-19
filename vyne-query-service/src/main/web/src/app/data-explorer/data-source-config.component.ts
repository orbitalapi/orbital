import {Component, EventEmitter, Input, Output} from '@angular/core';
import {UploadFile} from 'ngx-file-drop';
import {CsvOptions, XmlIngestionParameters} from '../services/types.service';
import {FormControl} from '@angular/forms';

@Component({
  selector: 'app-data-source-config',
  template: `
    <button mat-icon-button class="clear-button" (click)="clear.emit()">
      <img class="clear-icon" src="assets/img/clear-cross-circle.svg">
    </button>
    <span>{{ fileDataSource.relativePath }}</span>
    <button mat-icon-button class="configure-button" [matMenuTriggerFor]="configMenu" *ngIf="isRequireConfiguration">
      <mat-icon aria-hidden="false" >more_horiz</mat-icon>
    </button>

    <div class="spacer"></div>

    <mat-menu #configMenu="matMenu">
      <div class="config-menu" (click)="$event.stopPropagation()" *ngIf="isCsvContent">
        <mat-form-field>
          <mat-label>Separator</mat-label>
          <mat-select [(ngModel)]="csvOptions.separator" (selectionChange)="updateCsvOptions()">
            <mat-option *ngFor="let separator of separators" [value]="separator.value">
              {{separator.label}}
            </mat-option>
          </mat-select>
        </mat-form-field>
        <mat-checkbox [(ngModel)]="useSpecialValueForNull" (change)="updateCsvOptions()"
                      (click)="$event.stopPropagation();">Use special value for null
        </mat-checkbox>
        <mat-checkbox [(ngModel)]="dataContainsHeaders"
                      (ngModelChange)="onChangeDataContainsHeader($event)">Data contains headers
        </mat-checkbox>
        <mat-checkbox [(ngModel)]="csvOptions.containsTrailingDelimiters"
                      (click)="onChangeDataContainsTrailingDelimiters($event)">Data contains trailing delimiters
        </mat-checkbox>
        <mat-checkbox [(ngModel)]="dataHasContentToIgnore"
                      (click)="onChangeDataContainsHeader($event)">Data has prefix to ignore
        </mat-checkbox>
        <mat-form-field *ngIf="dataHasContentToIgnore" (click)="$event.stopPropagation()">
          <mat-label>Ignore everything before</mat-label>
          <input matInput placeholder="Start reading from this content"
                 [(ngModel)]="csvOptions.ignoreContentBefore"
                 (blur)="onContentPrefixChanged()"
                 (click)="$event.stopPropagation();">
        </mat-form-field>
        <mat-form-field *ngIf="useSpecialValueForNull" (click)="$event.stopPropagation();">
          <mat-label>Null tag</mat-label>
          <input matInput placeholder="Provide value to treat as null" [(ngModel)]="csvOptions.nullValueTag"
                 (click)="$event.stopPropagation();">
        </mat-form-field>
      </div>
      <div class="config-menu" (click)="$event.stopPropagation()" *ngIf="isXmlContent">
        <mat-form-field  (click)="$event.stopPropagation()">
          <mat-label>Collection selection xpath</mat-label>
          <input matInput placeholder=""
                 [(ngModel)]="xmlOptions.elementSelector"
                 (blur)="onCollectionSelectionXPathChanged()"
                 (click)="$event.stopPropagation();">
        </mat-form-field>
      </div>
    </mat-menu>
  `,
  styleUrls: ['./data-source-config.component.scss']
})
export class DataSourceConfigComponent {

  private _fileDataSource: UploadFile;
  extension: string;
  dataContainsHeaders = true;
  dataHasContentToIgnore = false;

  columnOne = new FormControl();
  columnTwo = new FormControl();
  xmlFileContainsCollection = false;

  get useSpecialValueForNull(): boolean {
    return this.csvOptions.nullValueTag !== null;
  }

  set useSpecialValueForNull(value: boolean) {
    this.csvOptions.nullValueTag = (value) ? 'null' : null;
  }

  get isCsvContent(): boolean {
    return CsvOptions.isCsvContent(this.extension);
  }

  get isXmlContent(): boolean {
    return XmlIngestionParameters.isXmlContent(this.extension);
  }

  get isRequireConfiguration(): boolean {
    return this.isCsvContent || this.isXmlContent;
  }

  @Output()
  clear = new EventEmitter<void>();

  csvOptions: CsvOptions = new CsvOptions();
  xmlOptions: XmlIngestionParameters = new XmlIngestionParameters();
  disabled = !this.csvOptions.firstRecordAsHeader;

  @Output()
  csvOptionsChanged = new EventEmitter<CsvOptions>();

  @Output()
  xmlOptionsChanged = new EventEmitter<XmlIngestionParameters>();

  separators = [
    {label: 'Comma ,', value: ','},
    {label: 'Semicolon ;', value: ';'},
    {label: 'Pipe |', value: '|'},
  ];

  @Input()
  get fileDataSource(): UploadFile {
    return this._fileDataSource;
  }

  set fileDataSource(value: UploadFile) {
    this._fileDataSource = value;
    const parts = value.relativePath.split('.');
    this.extension = parts[parts.length - 1];

    if (this.isCsvContent) {
      this.updateCsvOptions();
    }

    if (this.isXmlContent) {
      this.updateXmlOptions();
    }
  }

  updateCsvOptions() {
    this.csvOptionsChanged.emit(this.csvOptions);
  }

  updateXmlOptions() {
    this.xmlOptionsChanged.emit(this.xmlOptions);
  }

  submitColumnNamesForm($event) {
    $event.stopPropagation();
    this.updateCsvOptions();
  }

  onChangeFirstRowHasOffset($event) {
    $event.stopPropagation();
    this.csvOptions.firstRecordAsHeader = false;
  }

  onChangeFirstRowIsHeader($event) {
    $event.stopPropagation();
    this.csvOptions.firstRecordAsHeader = true;
    this.updateCsvOptions();
  }

  onChangeDataContainsHeader($event) {
    this.updateCsvOptions();
  }

  onChangeDataContainsTrailingDelimiters($event) {
    $event.stopPropagation();
    this.updateCsvOptions();
  }
  onContentPrefixChanged() {
    this.updateCsvOptions();
  }
  onCollectionSelectionXPathChanged($event) {
    $event.stopPropagation();
    this.updateXmlOptions();
  }
}
