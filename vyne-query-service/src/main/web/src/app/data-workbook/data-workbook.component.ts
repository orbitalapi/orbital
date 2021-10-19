import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, Type, TypeNamedInstance} from '../services/schema';
import {CsvOptions, CsvWithSchemaParseResponse, ParsedCsvContent, ParsedTypeInstance} from '../services/types.service';
import {FileSourceChangedEvent} from './data-source-panel/data-source-panel.component';
import {ParseTypeSelectedEvent} from './schema-selector/schema-selector.component';
import {InstanceSelectedEvent} from '../query-panel/instance-selected-event';
import {Observable} from 'rxjs/internal/Observable';
import {from} from 'rxjs';
import {isNullOrUndefined} from 'util';
import {ValueWithTypeName} from '../services/models';
import {QueryProfileData} from '../services/query.service';
import {QueryResultInstanceSelectedEvent} from '../query-panel/result-display/BaseQueryResultComponent';

@Component({
  selector: 'app-data-workbook',
  styleUrls: ['./data-workbook.component.scss'],
  template: `
    <app-source-mode-selector
      class="component"
      [schema]="schema"
      (csvDataUpdated)="onCsvDataUpdated($event)"
      (fileDataSourceChanged)="onFileSourceChanged($event)"
      [parsedCsvContent]="parsedCsvContent"
    ></app-source-mode-selector>

    <app-workbook-schema-selector
      *ngIf="fileSource"
      class="component"
      [schema]="schema"
      [typesInSchema]="typesInSchema"
      [working]="parseToTypeWorking"
      [errorMessage]="parseToTypeErrorMessage"
      (schemaChange)="parsingSchemaChange.emit($event)"
      (runClick)="onRunClicked($event)"
    ></app-workbook-schema-selector>

    <div class="parse-results mat-elevation-z1" *ngIf="parsingResults$">
      <div class="header">Parsing results</div>
      <app-object-view-container *ngIf="parsingResults$"
                                 [schema]="schema"
                                 [anonymousTypes]="typedParseResult.types"
                                 [selectable]="true"
                                 (instanceClicked)="onInstanceClicked($event)"
                                 [instances$]="parsingResults$"
                                 [type]="parsedContentType"
                                 #objectViewContainer
      ></app-object-view-container>
    </div>
    <app-tabbed-results-view *ngIf="projectingResultType"
                             [instances$]="projectingResults$"
                             [profileData$]="queryProfileData$"
                             [type]="projectingResultType"
                             [anonymousTypes]="typesInSchema"
                             (instanceSelected)="instanceSelected.emit($event)"
    ></app-tabbed-results-view>
  `
})
export class DataWorkbookComponent {

  @Input()
  schema: Schema;

  @Input()
  parsedCsvContent: ParsedCsvContent;

  private _typedParseResult: CsvWithSchemaParseResponse;
  parsingResults$: Observable<TypeNamedInstance>;

  @Input()
  projectingResults$: Observable<ValueWithTypeName>;
  @Input()
  queryProfileData$: Observable<QueryProfileData>;

  @Input()
  parsedContentType: Type;

  @Input()
  projectingResultType: Type;

  @Input()
  parseToTypeErrorMessage: string = null;
  @Input()
  parseToTypeWorking = false;

  @Input()
  get typedParseResult(): CsvWithSchemaParseResponse {
    return this._typedParseResult;
  }

  set typedParseResult(value: CsvWithSchemaParseResponse) {
    if (this._typedParseResult === value) {
      return;
    }
    this._typedParseResult = value;
    if (isNullOrUndefined(value)) {
      this.parsingResults$ = null;
      return;
    }
    const parsedTypedInstances = value.parsedTypedInstances;
    if (parsedTypedInstances instanceof Array) {
      this.parsingResults$ = from((parsedTypedInstances as ParsedTypeInstance[]).map(v => v.typeNamedInstance));
    } else {
      this.parsingResults$ = from([(parsedTypedInstances as ParsedTypeInstance).typeNamedInstance]);
    }
  }

  @Output()
  instanceSelected = new EventEmitter<QueryResultInstanceSelectedEvent>();

  @Output()
  fileDataSourceChanged = new EventEmitter<FileSourceChangedEvent>();
  fileSource: FileSourceChangedEvent;

  @Output()
  parsingSchemaChange = new EventEmitter<string>();

  @Output()
  projectingSchemaChange = new EventEmitter<string>();

  @Output()
  parseToType = new EventEmitter<ParseContentToTypeRequest>();

  @Output()
  projectToType = new EventEmitter<ParseContentToTypeRequest>();

  @Input()
  typesInSchema: Type[];

  @Input()
  typesInProjectionSchema: Type[];


  onFileSourceChanged($event: FileSourceChangedEvent, emitEvent: boolean = true) {
    this.fileSource = $event;
    if (emitEvent) {
      this.fileDataSourceChanged.emit($event);
    }

  }

  onRunClicked($event: ParseTypeSelectedEvent) {
    this.parsedCsvContent = null;
    this.typedParseResult = null;
    this.parsingResults$ = null;
    this.parseToType.emit(new ParseContentToTypeRequest(
      this.fileSource.contents,
      this.fileSource.csvOptions,
      $event
    ));
  }

  onInstanceClicked($event: InstanceSelectedEvent) {

  }

  onCsvDataUpdated($event: string) {
    // emitEvent = false, as we don't need to parse the results to
    // a table, since we already have it.
    // However, we do want to store the csv content for later, when the user
    // clicks "run"...
    this.onFileSourceChanged({
      contents: $event,
      extension: '.csv',
      fileName: 'local.csv',
      csvOptions: new CsvOptions()
    }, false);
  }

}

export class ParseContentToTypeRequest {
  constructor(public readonly contents: string,
              public readonly csvOptions: CsvOptions | null,
              public readonly selectedTypeEvent: ParseTypeSelectedEvent) {
  }

  get hasProjection(): boolean {
    return !isNullOrUndefined(this.selectedTypeEvent.projectionType);
  }
}
