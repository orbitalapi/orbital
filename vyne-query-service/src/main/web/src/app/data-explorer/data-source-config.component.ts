import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {UploadFile} from 'ngx-file-drop';
import {CsvOptions} from '../services/types.service';

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
      <div class="config-menu">
        <mat-form-field>
          <mat-label>Separator</mat-label>
          <mat-select [(ngModel)]="csvOptions.separator" (selectionChange)="updateCsvOptions()">
            <mat-option *ngFor="let separator of separators" [value]="separator.value">
              {{separator.label}}
            </mat-option>
          </mat-select>
        </mat-form-field>
        <mat-checkbox [(ngModel)]="csvOptions.firstRowAsHeader" (change)="updateCsvOptions()">First row is header
        </mat-checkbox>

        <mat-checkbox [(ngModel)]="useSpecialValueForNull" (change)="updateCsvOptions()"
                      (click)="$event.stopPropagation();">Use special value for null
        </mat-checkbox>
        <mat-form-field *ngIf="useSpecialValueForNull" (click)="$event.stopPropagation();">
          <mat-label>Null tag</mat-label>
          <input matInput placeholder="Provide value to treat as null" [(ngModel)]="csvOptions.nullValueTag"
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
}
