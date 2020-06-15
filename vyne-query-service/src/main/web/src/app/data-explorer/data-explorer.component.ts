import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {findType, Schema, Type} from '../services/schema';
import {
  VyneHttpServiceError,
  ParsedTypeInstance,
  TypesService,
  CsvOptions,
  ParsedCsvContent
} from '../services/types.service';
import {FileSystemEntry, FileSystemFileEntry, UploadFile} from 'ngx-file-drop';
import {HttpErrorResponse} from '@angular/common/http';
import {MatTabChangeEvent} from '@angular/material/tabs';
import {CodeViewerComponent} from '../code-viewer/code-viewer.component';
import {TypeNamedInstance} from '../services/query.service';
import {InstanceLike, typeName} from '../object-view/object-view.component';

@Component({
  selector: 'app-data-explorer',
  templateUrl: './data-explorer.component.html',
  styleUrls: ['./data-explorer.component.scss']
})
export class DataExplorerComponent {

  schemaLabel = 'Schema';
  schema: Schema;
  csvContents: ParsedCsvContent;
  fileContents: string;
  fileExtension: string;

  selectedTypeInstance: InstanceLike;
  selectedTypeInstanceType: Type;

  get showSidePanel(): boolean {
    return this.selectedTypeInstanceType !== undefined && this.selectedTypeInstance !== null;
  }

  set showSidePanel(value: boolean) {
    if (!value) {
      this.selectedTypeInstance = null;
    }
  }


  private _contentType: Type;
  parsedInstance: ParsedTypeInstance | ParsedTypeInstance[];
  typeNamedInstance: TypeNamedInstance | TypeNamedInstance[];

  parserErrorMessage: VyneHttpServiceError;

  @Output()
  parsedInstanceChanged = new EventEmitter<ParsedTypeInstance | ParsedTypeInstance[]>();
  csvOptions: CsvOptions = new CsvOptions();

  constructor(private typesService: TypesService) {
    this.typesService.getTypes()
      .subscribe(next => this.schema = next);
  }

  @ViewChild('appCodeViewer', {read: CodeViewerComponent, static: true})
  appCodeViewer: CodeViewerComponent;

  get isCsvContent(): boolean {
    if (!this.fileExtension) {
      return false;
    }
    return CsvOptions.isCsvContent(this.fileExtension);

  }

  @Input()
  get contentType(): Type {
    return this._contentType;
  }

  set contentType(value: Type) {
    this._contentType = value;
    this.parseToTypedInstanceIfPossible();
  }

  onFileSelected(uploadFile: UploadFile): void {
    if (!uploadFile.fileEntry.isFile) {
      throw new Error('Only files are supported');
    }

    this.fileExtension = this.getExtension(uploadFile);

    const fileEntry = uploadFile.fileEntry as FileSystemFileEntry;
    fileEntry.file(file => {
      const reader = new FileReader();
      reader.onload = ((event) => {
        this.fileContents = (event.target as any).result;
        if (this.isCsvContent) {
          this.parseCsvContentIfPossible();
        }
        this.parseToTypedInstanceIfPossible();
        this.parsedInstanceChanged.emit(null);
      });
      reader.readAsText(file);
    });


  }

  clearFile() {
    this.parsedInstance = null;
    this.contentType = null;
    this.fileContents = null;
    this.parserErrorMessage = null;
  }

  private parseCsvContentIfPossible() {
    if (!this.isCsvContent) {
      return;
    }
    if (!this.fileContents) {
      return;
    }
    this.typesService.parseCsv(this.fileContents, this.csvOptions)
      .subscribe(result => {
        this.parserErrorMessage = null;
        this.csvContents = result;
      }, error => {
        this.parserErrorMessage = (error as HttpErrorResponse).error as VyneHttpServiceError;
      });
  }

  private parseToTypedInstanceIfPossible() {
    if (!this.contentType || !this.fileContents) {
      return;
    }

    if (this.isCsvContent) {
      this.parseCsvToTypedInstance();
    } else {
      this.parseStringContentToTypedInstance();
    }

  }

  private parseCsvToTypedInstance() {
    this.typesService.parseCsvToType(this.fileContents, this.contentType, this.csvOptions)
      .subscribe(event => this.handleParsingResult(event), error => {
        this.parserErrorMessage = (error as HttpErrorResponse).error as VyneHttpServiceError;
        console.error('Failed to parse instance: ' + this.parserErrorMessage.message);
      });
  }

  private handleParsingResult(result: ParsedTypeInstance | ParsedTypeInstance[]) {
    this.parserErrorMessage = null;
    this.parsedInstance = result;

    if (result instanceof Array) {
      this.typeNamedInstance = (result as ParsedTypeInstance[]).map(v => v.typeNamedInstance);
    } else {
      this.typeNamedInstance = (result as ParsedTypeInstance).typeNamedInstance;
    }
    this.parsedInstanceChanged.emit(this.parsedInstance);


  }

  private parseStringContentToTypedInstance() {
    this.typesService.parse(this.fileContents, this.contentType)
      .subscribe(event => this.handleParsingResult(event), error => {
        this.parserErrorMessage = (error as HttpErrorResponse).error as VyneHttpServiceError;
        console.error('Failed to parse instance: ' + this.parserErrorMessage.message);
      });
  }

  onSelectedTabChanged(event: MatTabChangeEvent) {
    if (event.tab.textLabel === this.schemaLabel && this.appCodeViewer !== null) {
      this.appCodeViewer.remeasure();
    }
  }

  private getExtension(value: UploadFile): string {
    const parts = value.relativePath.split('.');
    return parts[parts.length - 1];
  }

  onCsvOptionsChanged(csvOptions: CsvOptions) {
    this.csvOptions = csvOptions;
    this.parseCsvContentIfPossible();
  }

  onInstanceClicked(event: InstanceLike) {
    this.selectedTypeInstance = event;
    const instanceTypeName = typeName(event);
    this.selectedTypeInstanceType = findType(this.schema, instanceTypeName);
    console.log('clicked: ' + JSON.stringify(event));
  }
}
