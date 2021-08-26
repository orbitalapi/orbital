import {Component, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {
  findType,
  Schema,
  Type,
  InstanceLike,
  getTypeName,
  TypeNamedInstance,
  isUntypedInstance, asNearestTypedInstance
} from '../services/schema';
import {
  VyneHttpServiceError,
  ParsedTypeInstance,
  TypesService,
  CsvOptions,
  ParsedCsvContent, XmlIngestionParameters
} from '../services/types.service';
import {FileSystemFileEntry, UploadFile} from 'ngx-file-drop';
import {HttpErrorResponse} from '@angular/common/http';
import {MatTabChangeEvent} from '@angular/material/tabs';
import {CodeViewerComponent} from '../code-viewer/code-viewer.component';
import {environment} from '../../environments/environment';
import {CaskService} from '../services/cask.service';
import {HeaderTypes} from './csv-viewer.component';
import {SchemaGeneratorComponent} from './schema-generator-panel/schema-generator.component';
import * as fileSaver from 'file-saver';
import {QueryFailure} from '../query-panel/query-wizard/query-wizard.component';
import {ExportFileService} from '../services/export.file.service';
import {MatDialog} from '@angular/material/dialog';
import {TestSpecFormComponent} from '../test-pack-module/test-spec-form.component';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {from, Observable} from 'rxjs/index';
import {Subject} from 'rxjs';
import {ObjectViewContainerComponent} from '../object-view/object-view-container.component';

@Component({
  selector: 'app-data-explorer',
  templateUrl: './data-explorer.component.html',
  styleUrls: ['./data-explorer.component.scss']
})
export class DataExplorerComponent {

  schemaTabLabel = 'Schema';
  parsedDataTabLabel = 'Parsed data';

  schema: Schema;
  csvContents: ParsedCsvContent;
  fileContents: string;
  fileExtension: string;

  selectedTypeInstance: InstanceLike;
  selectedTypeInstanceType: Type;
  shouldTypedInstancePanelBeVisible: boolean;

  @ViewChild(ObjectViewContainerComponent, {static: false})
  objectViewContainerComponent: ObjectViewContainerComponent;

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
  typeNamedInstance$: Observable<InstanceLike>;

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
  activeTab: number;
  xmlIngestionParameters: XmlIngestionParameters = new XmlIngestionParameters();

  constructor(private typesService: TypesService,
              private caskService: CaskService,
              private exportFileService: ExportFileService,
              private dialogService: MatDialog,
              private schemaNotificationService: SchemaNotificationService) {
    this.typesService.getTypes()
      .subscribe(next => {
        console.log('Data explorer received a new schema');
        this.schema = next;
      });
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(() => this.onSchemaUpdated());
    this.caskServiceUrl = environment.queryServiceUrl;
  }

  @ViewChild('appCodeViewer', {read: CodeViewerComponent, static: false})
  appCodeViewer: CodeViewerComponent;

  @ViewChild('schemaGenerator', {
    read: SchemaGeneratorComponent,
    static: false
  })
  schemaGenerationPanel: SchemaGeneratorComponent;


  caskServiceUrl: string;

  get isCsvContent(): boolean {
    if (!this.fileExtension) {
      return false;
    }
    return CsvOptions.isCsvContent(this.fileExtension);
  }

  get isXmlContent(): boolean {
    return XmlIngestionParameters.isXmlContent(this.fileExtension);
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

  onSchemaUpdated() {
    this.parseToTypedInstanceIfPossible();
  }

  showTypeNamePanel($event) {
    this.isTypeNamePanelVisible = $event;
    return this.isTypeNamePanelVisible;
  }

  showGenerateSchemaPanel($event) {
    this.isGenerateSchemaPanelVisible = $event;
    if (this.isGenerateSchemaPanelVisible) {
      setTimeout(() => {
        this.schemaGenerationPanel.generateSchema();
      }, 0);
    }
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
    } else if (this.isXmlContent) {
      this.parseXmlToTypedInstances();
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

  private parseXmlToTypedInstances() {
    this.typesService.parseXmlToType(this.fileContents, this.contentType, this.xmlIngestionParameters)
      .subscribe(parsedTypedInstant => this.handleParsingResult(parsedTypedInstant),
        error => {
          this.parserErrorMessage = (error as HttpErrorResponse).error as VyneHttpServiceError;
          console.error('Failed to parse instance: ' + this.parserErrorMessage.message);
        });
  }

  private handleParsingResult(result: ParsedTypeInstance | ParsedTypeInstance[]) {
    this.parserErrorMessage = null;
    if (result instanceof Array) {
      this.typeNamedInstance$ = from((result as ParsedTypeInstance[]).map(v => v.typeNamedInstance));
    } else {
      this.typeNamedInstance$ = from([(result as ParsedTypeInstance).typeNamedInstance]);
    }
    this.parsedInstance = result;
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
    this.activeTab = event.tab.origin;
    if (event.tab.textLabel === this.parsedDataTabLabel && this.objectViewContainerComponent) {
      setTimeout(() => {
        this.objectViewContainerComponent.remeasureTable();
      });
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

  onXmlOptionsChanged(xmlOptions: XmlIngestionParameters) {
    this.xmlIngestionParameters = xmlOptions;
    this.parseCsvContentIfPossible();
  }

  onInstanceClicked(event: InstanceSelectedEvent) {
    if (isUntypedInstance(event.selectedTypeInstance) && event.selectedTypeInstance.nearestType !== null) {
      const typedInstance = asNearestTypedInstance(event.selectedTypeInstance);
      this.shouldTypedInstancePanelBeVisible = true;
      this.selectedTypeInstance = typedInstance;
      this.selectedTypeInstanceType = typedInstance.type;
    } else if (event.selectedTypeInstanceType !== null) {
      this.shouldTypedInstancePanelBeVisible = true;
      this.selectedTypeInstance = event.selectedTypeInstance as InstanceLike;
      this.selectedTypeInstanceType = event.selectedTypeInstanceType;
    }

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
