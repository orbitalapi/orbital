import {ChangeDetectorRef, Component} from '@angular/core';
import {ContentWithSchemaParseResponse, CsvOptions, ParsedCsvContent, TypesService} from '../services/types.service';
import {Observable} from 'rxjs/internal/Observable';
import {Type} from '../services/schema';
import {FileSourceChangedEvent} from './data-source-panel/data-source-panel.component';
import {ReplaySubject, Subject} from 'rxjs';
import {debounceTime, throttleTime} from 'rxjs/operators';
import {ParseContentToTypeRequest} from './data-workbook.component';
import {ValueWithTypeName} from '../services/models';
import {QueryProfileData, QueryService, randomId} from '../services/query.service';
import {BaseQueryResultDisplayComponent} from '../query-panel/BaseQueryResultDisplayComponent';

@Component({
  selector: 'app-data-workbook-container',
  template: `
    <div class="page-content container">
      <app-data-workbook
        (fileDataSourceChanged)="onFileSourceChanged($event)"
        [schema]="schema"
        (parsingSchemaChange)="debouncedParsingSchemaChanges.next($event)"
        [typesInSchema]="typesInParsingSchema"
        [parsedCsvContent]="parseResult"
        (parseToType)="onParseContentToType($event)"
        (projectingSchemaChange)="debouncedProjectingSchemaChanges.next($event)"
        [parsedContentType]="parsedContentType"
        [typedParseResult]="typedParseResult"
        [projectingResultType]="projectedContentType"
        [projectingResults$]="projectingQueryResults$"
        [queryProfileData$]="queryProfileData$"
        (instanceSelected)="onQueryResultSelected($event)"
        [parseToTypeWorking]="parseToTypeWorking"
        [parseToTypeErrorMessage]="parseToTypeErrorMessage"
      ></app-data-workbook>
    </div>
  `,
  styleUrls: ['./data-workbook-container.component.scss']
})
export class DataWorkbookContainerComponent extends BaseQueryResultDisplayComponent {

  parseResult: ParsedCsvContent;
  typesInParsingSchema: Type[];

  private parsingSchemaContent: string;
  schemaParseError: string;
  parsedContentType: Type;
  typedParseResult: ContentWithSchemaParseResponse;

  projectingSchemaContent: string;
  typesInProjectingSchema: Type[];
  projectedContentType: Type;
  queryProfileData$: Observable<QueryProfileData>;

  // Use a replay subject here, so that when people switch
  // between Query Results and Profiler tabs, the results are still made available
  projectingQueryResults$ = new ReplaySubject<ValueWithTypeName>(5000);


  debouncedParsingSchemaChanges = new Subject<string>();
  debouncedProjectingSchemaChanges = new Subject<string>();

  parseToTypeErrorMessage: string = null;
  parseToTypeWorking = false;

  constructor(typeService: TypesService, queryService: QueryService, changeDetector: ChangeDetectorRef) {
    super(queryService, typeService, changeDetector);
    this.debouncedParsingSchemaChanges
      .pipe(debounceTime(1000))
      .subscribe(schema => {
        this.parsingSchemaContent = schema;
        this.validateParsingSchema(schema);
      });
    this.debouncedProjectingSchemaChanges
      .pipe(throttleTime(1000))
      .subscribe(schema => {
        this.validateProjectingSchema(schema, this.parsingSchemaContent);
      });

  }

  onFileSourceChanged($event: FileSourceChangedEvent) {
    if (CsvOptions.isCsvContent($event.extension)) {
      this.typeService.parseCsv(
        $event.contents, $event.csvOptions
      ).subscribe(parseResult => {
          this.parseResult = parseResult;
        },
        error => {
          console.log(JSON.stringify(error));
          // this.parseError = error;?
        });
    } else {
      this.parseResult = null;
    }

  }

  private validateParsingSchema(schema: string) {
    this.typeService.validateSchema(schema)
      .subscribe(types => this.typesInParsingSchema = types,
        error => {
          console.log('Schema parse failed: ' + JSON.stringify(error));
        }
      );
  }

  private validateProjectingSchema(projectingSchema: string, parsingSchema: string) {
    const combinedSchema = projectingSchema + '\n' + parsingSchema;
    this.typeService.validateSchema(combinedSchema)
      .subscribe(types => this.typesInProjectingSchema = types,
        error => {
          console.log('Schema parse failed: ' + JSON.stringify(error));
        }
      );

  }

  onParseContentToType($event: ParseContentToTypeRequest) {
    this.parseToTypeErrorMessage = null;
    this.parseToTypeWorking = true;
    this.parsedContentType = null;
    const isJson = $event.contents.startsWith('[') || $event.contents.startsWith('{');
    // For readability.
    // We'll kill off this isCsv / isJson stuff when model formats are merged to develop.
    const isCsv = !isJson;

    if (isCsv && $event.hasProjection) {
      this.parseCsvProjection($event);
    } else if ($event.hasProjection) {
      this.parseContentWithProjection($event);
    } else if ($event.selectedTypeEvent.schema) {
      const observable = (isCsv) ? this.parseCsvContentWithSchema($event) : this.parseContentWithSchema($event);
      observable.subscribe(parseResult => {
        this.parseToTypeWorking = false;
        this.typedParseResult = parseResult;
        this.parsedContentType = $event.selectedTypeEvent.parseType;
      }, error => {
        this.parseToTypeErrorMessage = error.error.message;
        this.parseToTypeWorking = false;
      });
    } else {
      const observable = (isCsv) ?
        this.typeService.parseCsvToType($event.contents, $event.selectedTypeEvent.parseType, $event.csvOptions)
        : this.typeService.parse($event.contents, $event.selectedTypeEvent.parseType);
      observable.subscribe(parseResult => {
        this.parseToTypeWorking = false;
        this.parsedContentType = $event.selectedTypeEvent.parseType;
        this.typedParseResult = {
          types: null,
          parsedTypedInstances: parseResult
        };
      }, error => {
        this.parseToTypeWorking = false;
        this.parseToTypeErrorMessage = error.error.message;
      });
    }
  }

  private parseContentWithSchema($event: ParseContentToTypeRequest): Observable<ContentWithSchemaParseResponse> {
    return this.typeService.parseContentToTypeWithAdditionalSchema(
      $event.contents,
      $event.selectedTypeEvent.parseType.name.parameterizedName,
      $event.selectedTypeEvent.schema
    );
  }

  private parseCsvContentWithSchema($event: ParseContentToTypeRequest) {
    return this.typeService.parseCsvToTypeWithAdditionalSchema(
      $event.contents,
      $event.selectedTypeEvent.parseType.name.parameterizedName,
      $event.csvOptions,
      $event.selectedTypeEvent.schema
    );
  }

  private parseCsvProjection($event: ParseContentToTypeRequest) {
    const queryId = randomId();
    this.projectingQueryResults$ = new ReplaySubject(5000);
    this.projectedContentType = $event.selectedTypeEvent.projectionType;
    this.typeService.parseCsvToProjectedTypeWithAdditionalSchema(
      $event.contents,
      $event.selectedTypeEvent.parseType.name.parameterizedName,
      $event.selectedTypeEvent.projectionType.name.parameterizedName,
      $event.csvOptions,
      $event.selectedTypeEvent.schema,
      queryId
    ).subscribe(message => {
        this.parseToTypeWorking = false;
        this.projectingQueryResults$.next(message);
      },
      error => {
        this.parseToTypeErrorMessage = error.error.message;
        this.parseToTypeWorking = false;
      },
      () => this.loadQueryProfileData(queryId));
  }

  get queryId(): string {
    throw new Error('Method not implemented.');
  }

  private loadQueryProfileData(queryId: string) {
    this.queryProfileData$ = this.queryService.getQueryProfileFromClientId(queryId);
  }

  private parseContentWithProjection($event: ParseContentToTypeRequest) {

  }
}
