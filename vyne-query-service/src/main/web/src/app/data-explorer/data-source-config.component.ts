import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {UploadFile} from 'ngx-file-drop';

@Component({
  selector: 'app-data-source-config',
  template: `
    <button mat-icon-button class="clear-button" (click)="clear.emit()">
      <img class="clear-icon" src="assets/img/clear-cross-circle.svg">
    </button>
    <span>{{ fileDataSource.relativePath }}</span>
    <app-file-extension-icon [extension]="extension"></app-file-extension-icon>

    <button mat-icon-button class="configure-button">
      <img class="configure-icon" src="assets/img/more-dots.svg">
    </button>
  `,
  styleUrls: ['./data-source-config.component.scss']
})
export class DataSourceConfigComponent {

  private _fileDataSource: UploadFile;
  extension: string;

  @Output()
  clear = new EventEmitter<void>();

  @Input()
  get fileDataSource(): UploadFile {
    return this._fileDataSource;
  }

  set fileDataSource(value: UploadFile) {
    this._fileDataSource = value;
    const parts = value.relativePath.split('.');
    this.extension = parts[parts.length - 1];
  }
}
