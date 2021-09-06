import {Component, OnInit} from '@angular/core';
import {CsvOptions, CsvWithSchemaParseResponse, ParsedCsvContent, TypesService} from '../services/types.service';
import {Observable} from 'rxjs/internal/Observable';
import {
  findType,
  isTypedInstance,
  isTypeNamedInstance,
  isUntypedInstance,
  Schema,
  Type,
  TypeNamedInstance
} from '../services/schema';
import {FileSourceChangedEvent} from './data-source-panel/data-source-panel.component';
import {ReplaySubject, Subject} from 'rxjs';
import {throttleTime} from 'rxjs/operators';
import {ParseContentToTypeRequest} from './data-workbook.component';
import {ValueWithTypeName} from '../services/models';
import {QueryResultInstanceSelectedEvent} from '../query-panel/result-display/BaseQueryResultComponent';
import {QueryProfileData, QueryService, randomId} from '../services/query.service';
import {BaseQueryResultDisplayComponent} from '../query-panel/BaseQueryResultDisplayComponent';

@Component({
  selector: 'app-data-workbook-container',
  template: `
    <app-header-bar title="Data workbook">
    </app-header-bar>
    <mat-sidenav-container [hasBackdrop]="true">
      <mat-drawer #drawer mode="over" position="end"
                  [(opened)]="showSidePanel && this.shouldTypedInstancePanelBeVisible">
        <div class="drawer-content">
          <app-typed-instance-panel-container
            [instance]="selectedTypeInstance"
            (hasTypedInstanceDrawerClosed)="shouldTypedInstancePanelBeVisible = $event"
            [type]="selectedTypeInstanceType"
            [dataSource]="selectedTypeInstanceDataSource"
            [instanceQueryCoordinates]="selectedInstanceQueryCoordinates"
          ></app-typed-instance-panel-container>
        </div>
      </mat-drawer>
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
          (instanceSelected)="onInstanceSelected($event)"
        ></app-data-workbook>
      </div>
    </mat-sidenav-container>
  `,
  styleUrls: ['./data-workbook-container.component.scss']
})
export class DataWorkbookContainerComponent extends BaseQueryResultDisplayComponent {

  parseResult: ParsedCsvContent;
  typesInParsingSchema: Type[];

  private parsingSchemaContent: string;
  schemaParseError: string;
  parsedContentType: Type;
  typedParseResult: CsvWithSchemaParseResponse;

  projectingSchemaContent: string;
  typesInProjectingSchema: Type[];
  projectedContentType: Type;
  queryProfileData$: Observable<QueryProfileData>;

  // Use a replay subject here, so that when people switch
  // between Query Results and Profiler tabs, the results are still made available
  projectingQueryResults$ = new ReplaySubject<ValueWithTypeName>(5000);


  debouncedParsingSchemaChanges = new Subject<string>();
  debouncedProjectingSchemaChanges = new Subject<string>();

  constructor(typeService: TypesService, queryService: QueryService) {
    super(queryService, typeService);
    this.debouncedParsingSchemaChanges
      .pipe(throttleTime(1000))
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
    if ($event.hasProjection) {
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
          this.projectingQueryResults$.next(message);
        },
        error => console.error('projecting query failed: ' + JSON.stringify(error)),
        () => this.loadQueryProfileData(queryId)
      );
    } else if ($event.selectedTypeEvent.schema) {
      this.typeService.parseCsvToTypeWithAdditionalSchema(
        $event.contents,
        $event.selectedTypeEvent.parseType.name.parameterizedName,
        $event.csvOptions,
        $event.selectedTypeEvent.schema
      )
        .subscribe(parseResult => {
          this.typedParseResult = parseResult;
          this.parsedContentType = $event.selectedTypeEvent.parseType;
        });
    } else {
      this.typeService.parseCsvToType($event.contents, $event.selectedTypeEvent.parseType, $event.csvOptions)
        .subscribe(parseResult => {
          this.typedParseResult = {
            types: null,
            parsedTypedInstances: parseResult
          };
        });
    }
  }

  get queryId(): string {
    throw new Error('Method not implemented.');
  }

  private loadQueryProfileData(queryId: string) {
    this.queryProfileData$ = this.queryService.getQueryProfileFromClientId(queryId);
  }
}
