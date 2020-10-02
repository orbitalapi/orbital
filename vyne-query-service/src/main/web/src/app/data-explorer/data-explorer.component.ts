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
import {QueryResult, TypeNamedInstance} from '../services/query.service';
import {InstanceLike, typeName} from '../object-view/object-view.component';
import {environment} from '../../environments/environment';
import {CaskService} from '../services/cask.service';
import {HeaderTypes} from './csv-viewer.component';
import {SchemaGeneratorComponent} from './schema-generator-panel/schema-generator.component';
import * as fileSaver from 'file-saver';
import {QueryFailure} from '../query-panel/query-wizard/query-wizard.component';
import {ExportFileService} from '../services/export.file.service';
import {DownloadFileType} from '../query-panel/result-display/result-container.component';
import {MatDialog} from '@angular/material/dialog';
import {TestSpecFormComponent} from './test-spec-form.component';

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
  shouldTypedInstancePanelBeVisible: boolean;

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
  isTypeNamePanelVisible = false;
  @Output()
  isGenerateSchemaPanelVisible = false;
  @Output()
  parsedInstanceChanged = new EventEmitter<ParsedTypeInstance | ParsedTypeInstance[]>();
  csvOptions: CsvOptions = new CsvOptions();
  headersWithAssignedTypes: HeaderTypes[] = [];
  assignedTypeName: string;


  constructor(private typesService: TypesService,
              private caskService: CaskService,
              private exportFileService: ExportFileService,
              private dialogService: MatDialog) {
    this.typesService.getTypes()
      .subscribe(next => this.schema = next);
    this.caskServiceUrl = environment.queryServiceUrl;
  }

  @ViewChild('appCodeViewer', {read: CodeViewerComponent, static: false})
  @ViewChild('schemaGenerator', {
    read: SchemaGeneratorComponent,
    static: false
  }) schemaGenerationPanel: SchemaGeneratorComponent;

  appCodeViewer: CodeViewerComponent;
  caskServiceUrl: string;

  get isCsvContent(): boolean {
    if (!this.fileExtension) {
      return false;
    }
    return CsvOptions.isCsvContent(this.fileExtension);

  }

  get isGenerateSchemaPanelOpen(): boolean {
    return !!(this.isGenerateSchemaPanelVisible && this.fileExtension);
  }

  get isTypeNamePanelOpen(): boolean {
    return !!(this.isTypeNamePanelVisible && this.fileExtension);
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
    this.showTypeNamePanel(false);
    this.showGenerateSchemaPanel(false);

  }

  showTypeNamePanel($event) {
    this.isTypeNamePanelVisible = $event;
    return this.isTypeNamePanelVisible;
  }

  showGenerateSchemaPanel($event) {
    this.isGenerateSchemaPanelVisible = $event;
    setTimeout(() => {
      this.schemaGenerationPanel.generateSchema();
    }, 0);
    return this.isGenerateSchemaPanelVisible;
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
    if (event.tab.textLabel === this.schemaLabel && this.appCodeViewer) {
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
    this.parseToTypedInstanceIfPossible();
  }

  onInstanceClicked(event: InstanceLike) {
    this.shouldTypedInstancePanelBeVisible = true;
    this.selectedTypeInstance = event;
    const instanceTypeName = typeName(event);
    this.selectedTypeInstanceType = findType(this.schema, instanceTypeName);
    console.log('clicked: ' + JSON.stringify(event));
  }

  onTypeNameChanged($event: string) {
    this.assignedTypeName = $event;
  }

  getHeadersWithAssignedTypes($event: any) {
    this.headersWithAssignedTypes = $event;
  }

  onCloseTypedInstanceDrawer($event: boolean) {
    this.shouldTypedInstancePanelBeVisible = $event;
  }

  onDownloadParsedDataClicked() {
    this.exportFileService.exportParsedData(this.fileContents, this.contentType, this.csvOptions, false)
      .subscribe(response => {
        const blob: Blob = new Blob([response], {type: `text/json; charset=utf-8`});
        fileSaver.saveAs(blob, `parsed-data-${new Date().getTime()}.json`);
      });
  }

  onDownloadTestSpecClicked() {
    const dialogRef = this.dialogService.open(TestSpecFormComponent, {
      width: '550px'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result !== null) {
        // noinspection UnnecessaryLocalVariableJS
        const specName = result;
        this.exportFileService.exportTestSpec(this.fileContents, this.contentType, this.csvOptions, specName)
          .subscribe(response => {
            const blob: Blob = new Blob([response], {type: `application/zip`});
            fileSaver.saveAs(blob, `${specName}-spec-${new Date().getTime()}.zip`);
          });
      }
    });

  }
}
