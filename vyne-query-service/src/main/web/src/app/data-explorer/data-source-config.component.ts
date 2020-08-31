import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {UploadFile} from 'ngx-file-drop';
import {CsvOptions} from '../services/types.service';
import {FormBuilder, FormControl, FormGroup} from '@angular/forms';

@Component({
  selector: 'app-data-source-config',
  template: `
    <button mat-icon-button class="clear-button" (click)="clear.emit()">
      <img class="clear-icon" src="assets/img/clear-cross-circle.svg">
    </button>
    <span>{{ fileDataSource.relativePath }}</span>
    <app-file-extension-icon [extension]="extension"></app-file-extension-icon>

    <button mat-icon-button class="configure-button" [matMenuTriggerFor]="configMenu" *ngIf="isCsvContent">
      <img class="configure-icon" src="assets/img/more-dots.svg">
    </button>
    <mat-menu #configMenu="matMenu">
      <div class="config-menu" (click) = "$event.stopPropagation()">
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
                      (click)="onChangeDataContainsHeader($event)">Data contains headers
        </mat-checkbox>
        <div *ngIf="dataContainsHeaders">
          <mat-radio-group class="example-radio-group">
            <mat-radio-button value="csvOptions.firstRowAsHeader" checked="csvOptions.firstRowAsHeader"
                              (click)="onChangeFirstRowIsHeader($event)">
              First row is header
            </mat-radio-button>
            <mat-radio-button value="csvOptions.firstRowHasOffset" (click)="onChangeFirstRowHasOffset($event)">
              Specify first two columns
            </mat-radio-button>
          </mat-radio-group>
        </div>
        <mat-form-field *ngIf="useSpecialValueForNull" (click)="$event.stopPropagation();">
          <mat-label>Null tag</mat-label>
          <input matInput placeholder="Provide value to treat as null" [(ngModel)]="csvOptions.nullValueTag"
                 (click)="$event.stopPropagation();">
        </mat-form-field>
        <form [formGroup]="columnNameForm" *ngIf="dataContainsHeaders && csvOptions.firstRowHasOffset"
              class="column-name-input-form">
          <mat-form-field class="add-column-name-text-field">
            <input matInput placeholder="Enter first column name"
                   [formControl]="columnOne"
                   required
                   (click)="$event.stopPropagation()"/>
          </mat-form-field>
          <mat-form-field class="add-column-name-text-field">
            <input matInput placeholder="Enter second column name"
                   required
                   [formControl]="columnTwo"
                   (click)="$event.stopPropagation()"/>
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="submitColumnNamesForm($event)">Submit</button>
          <button mat-stroked-button (click)="resetColumnNameForm($event)" style="margin-left: 0.4em">Clear</button>
        </form>
      </div>
    </mat-menu>
  `,
  styleUrls: ['./data-source-config.component.scss']
})
export class DataSourceConfigComponent {
  constructor(private fb: FormBuilder) {
    this.columnNameForm = fb.group({
      columnOne: this.columnOne,
      columnTwo: this.columnTwo,
    });
  }

  private _fileDataSource: UploadFile;
  extension: string;
  dataContainsHeaders = true;
  columnNameForm: FormGroup;

  columnOne = new FormControl();
  columnTwo = new FormControl();

  get useSpecialValueForNull(): boolean {
    return this.csvOptions.nullValueTag !== null;
  }

  set useSpecialValueForNull(value: boolean) {
    this.csvOptions.nullValueTag = (value) ? 'null' : null;
  }

  get isCsvContent() {
    return CsvOptions.isCsvContent(this.extension);
  }

  @Output()
  clear = new EventEmitter<void>();

  csvOptions: CsvOptions = new CsvOptions();
  disabled = !this.csvOptions.firstRowHasOffset && !this.csvOptions.firstRecordAsHeader;

  @Output()
  csvOptionsChanged = new EventEmitter<CsvOptions>();

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

    if (CsvOptions.isCsvContent(this.extension)) {
      this.updateCsvOptions();
    }
  }

  updateCsvOptions() {
    this.csvOptionsChanged.emit(this.csvOptions);
  }

  submitColumnNamesForm($event) {
    $event.stopPropagation();
    this.csvOptions.columnOneName = this.columnOne.value;
    this.csvOptions.columnTwoName = this.columnTwo.value;
    if (this.csvOptions.columnOneName.length && this.csvOptions.columnTwoName.length) {
      this.updateCsvOptions();
    }
  }

  onChangeFirstRowHasOffset($event) {
    $event.stopPropagation();
    this.csvOptions.firstRowHasOffset = true;
    this.csvOptions.firstRecordAsHeader = false;
  }

  onChangeFirstRowIsHeader($event) {
    $event.stopPropagation();
    this.csvOptions.firstRowHasOffset = false;
    this.csvOptions.firstRecordAsHeader = true;
    this.updateCsvOptions();
  }

  onChangeDataContainsHeader($event) {
    $event.stopPropagation();
    this.resetColumnNameForm();
    this.csvOptions.firstRecordAsHeader = !this.dataContainsHeaders;
    this.csvOptions.firstRowHasOffset = this.dataContainsHeaders ? this.csvOptions.firstRowHasOffset : false;
    this.updateCsvOptions();
  }

  resetColumnNameForm($event?) {
    if ($event) {
      $event.stopPropagation();
    }
    this.csvOptions.columnOneName = '';
    this.csvOptions.columnTwoName = '';
    this.columnNameForm.reset();
  }
}
