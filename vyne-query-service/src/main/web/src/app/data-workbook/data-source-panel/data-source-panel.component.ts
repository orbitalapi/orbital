import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FileSystemFileEntry, UploadFile} from 'ngx-file-drop';
import {CsvOptions, ParsedCsvContent} from '../../services/types.service';
import {Schema} from '../../services/schema';
import {getExtension} from '../../data-explorer/data-explorer.component';

@Component({
  selector: 'app-data-source-panel',
  template: `
    <div class="container mat-elevation-z2">
      <div class="upload-container" *ngIf="!fileDataSource">
        <app-data-source-upload (fileSelected)="onFileSelected($event)"

        ></app-data-source-upload>
      </div>
    </div>
    <mat-expansion-panel *ngIf="fileContents">
      <mat-expansion-panel-header>
        <mat-panel-title>
          <div class="description-container">
            <button mat-icon-button (click)="onClearClicked($event)">
              <img class="clear-icon" src="assets/img/clear-cross-circle.svg">
            </button>
            Source: {{ fileDataSource.relativePath }}
          </div>
        </mat-panel-title>
      </mat-expansion-panel-header>

      <ng-template matExpansionPanelContent>
        <app-data-source-display [fileContents]="fileContents"
                                 [fileExtension]="fileExtension"
                                 [csvContents]="parsedCsvContent"
                                 [schema]="schema"
                                 [(csvOptions)]="csvOptions"
                                 [fileDataSource]="fileDataSource"
        ></app-data-source-display>
      </ng-template>
    </mat-expansion-panel>
  `,
  styleUrls: ['./data-source-panel.component.scss']
})
export class DataSourcePanelComponent {

  @Output()
  cleared = new EventEmitter<void>();

  @Input()
  schema: Schema;

  @Input()
  csvOptions: CsvOptions = new CsvOptions();

  @Input()
  fileDataSource: UploadFile;

  @Input()
  fileContents: string;

  @Input()
  fileExtension: string;

  @Input()
  parsedCsvContent: ParsedCsvContent;

  @Output()
  fileDataSourceChanged = new EventEmitter<FileSourceChangedEvent>();

  clearSelectedFile() {
    this.fileDataSource = null;
    this.cleared.emit();
  }

  get isCsvContent(): boolean {
    if (!this.fileExtension) {
      return false;
    }
    return CsvOptions.isCsvContent(this.fileExtension);
  }

  onFileSelected(uploadFile: UploadFile) {
    this.fileDataSource = uploadFile;
    if (!uploadFile.fileEntry.isFile) {
      throw new Error('Only files are supported');
    }

    this.fileExtension = getExtension(uploadFile);

    const fileEntry = uploadFile.fileEntry as FileSystemFileEntry;
    fileEntry.file(file => {
      const reader = new FileReader();
      reader.onload = ((event) => {
        this.fileContents = (event.target as any).result;
        this.fileDataSourceChanged.emit({
          contents: this.fileContents,
          extension: this.fileExtension,
          fileName: this.fileDataSource.relativePath,
          csvOptions: this.csvOptions
        });
      });
      reader.readAsText(file);
    });
  }

  onClearClicked($event: MouseEvent) {
    $event.stopImmediatePropagation();
    this.clearSelectedFile();
  }
}

export interface FileSourceChangedEvent {
  fileName: string;
  extension: string;
  contents: string;
  csvOptions: CsvOptions | null;
}
