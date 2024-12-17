import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input, NgZone,
  OnInit,
  Output, ViewContainerRef
} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import Prism from 'prismjs'
import 'prismjs/plugins/toolbar/prism-toolbar';
import {BehaviorSubject, combineLatest, Observable, of, ReplaySubject} from 'rxjs';
import {filter, map, startWith, tap} from 'rxjs/operators';
import {
  collectAllServiceOperations,
  findType,
  Message,
  Operation,
  PartialSchema,
  Schema,
  ServiceMember,
  Type,
  VersionedSource,
} from 'src/app/services/schema';
import {UiCustomisations} from '../../environments/ui-customisations';
import {QueryPanelStoreService} from '../services/query-panel-store.service';
import {combineAndCloneWithPartialSchema, SchemaSubmissionResult} from '../services/types.service';
import {SchemaEditOperation} from '../project-import/schema-importer.service';
import {CodeViewerFlexBoxMode} from '../code-viewer/code-viewer.component';
import {
  Links,
  buildLinksForModelWithAttributes,
  buildLinksForType,
  buildOperationLinks,
  findServiceAssociatedWithOperation,
} from '../schema-diagram/schema-chart-builder';
import {taxi} from '../utils/prism.languages';
import {MarkdownService} from "ngx-markdown";
import {CustomMarkdownRenderer} from "../markdown-utils/markdown-custom-renderer";
import {InlineRunQueryButtonComponent} from "../inline-run-query-button/inline-run-query-button.component";

@Component({
  selector: 'app-schema-member-type-explorer',
  template: `
    <div class="main-content">
      <tui-tabs [(activeItemIndex)]="activeTabIndex" *ngIf="hasCodeView" class="schema-source-tabs">
        <button tuiTab>
          <img src="assets/img/tabler/table.svg" class="icon">
          Schema
        </button>
        <button tuiTab>
          <img src="assets/img/tabler/code.svg" class="icon">
          Source
        </button>
      </tui-tabs>
      <as-split direction="horizontal" unit="pixel" *ngIf="activeTabIndex === 0">
        <as-split-area size="260" maxSize="360">
          <app-schema-member-tree [partialSchema$]="partialSchema$" #schemaEntryTable
                                  (modelSelected)="onModelSelected($event)"
                                  (operationSelected)="onOperationSelected($event)"
                                  (resetSelection)="onResetMemberSelection()"
          ></app-schema-member-tree>
        </as-split-area>
        <as-split-area size="*">
          <as-split direction="horizontal">
            <as-split-area [size]="selectedOperation ? 100 : 50">
              <div class="documentation-content-container">
                <div class="documentation-content">
                  <app-type-viewer *ngIf="selectedModel"
                                   [type]="selectedModel"
                                   [schema]="schema"
                                   [partialSchema]="partialSchema"
                                   [showUsages]="false"
                                   [showContentsList]="false"
                                   [anonymousTypes]="partialSchema?.types"
                                   commitMode="explicit"
                                   [editable]="editable"
                                   [schemaMemberNavigable]="!editable"
                                   (newTypeCreated)="handleNewTypeCreated($event, selectedModel)"
                                   (typeUpdated)="handleSchemaEditOperation($event.schemaEditOperation, $event.member, selectedModel)"
                  ></app-type-viewer>
                  <app-operation-view *ngIf="selectedOperation"
                                      [operation]="selectedOperation"
                                      [schema]="schema"
                                      [allowTryItOut]="allowTryItOut"
                                      [editable]="editable"
                                      [schemaMemberNavigable]="!editable"
                                      commitMode="explicit"
                                      (newTypeCreated)="handleNewTypeCreated($event, selectedOperation)"
                                      (updateDeferred)="handleSchemaEditOperation($event.schemaEditOperation, $event.member, selectedOperation)"
                  ></app-operation-view>
                  <div *ngIf="!selectedModel && !selectedOperation" appCaptureLocalNavigation class="readme-diagram-container">
                    <tui-segmented size="s" [(activeItemIndex)]="readmeDiagramSegmentIndex">
                      <button type="button">Readme</button>
                      <button type="button">Project diagram</button>
                    </tui-segmented>
                    <ng-container *ngIf="readmeDiagramSegmentIndex === 0">
                      @if (readme) {
                        <markdown [data]="readme" class="markdown-body" [disableSanitizer]="true"/>
                      } @else {
                        <div class="add-readme-call-to-action">
                          <img [tuiSkeleton]="isReadmeLoading && 2" src="assets/img/tabler/book.svg" class="book-icon">
                          <h2 [tuiSkeleton]="isReadmeLoading ? 'Add a README' : ''">{{isReadmeLoading ? '' : 'Add a README'}}</h2>
                          <div [tuiSkeleton]="isReadmeLoading && 70">
                            There's no README for this project. Markdown is supported (including Github flavoured markdown goodies).<br/>
                            <a [href]="UiCustomisations.docsLinks.projectReadme" target="_blank">Learn more</a> about adding README's to projects.
                          </div>
                          <div [tuiSkeleton]="isReadmeLoading && 30">(or select a schema member from the panel on the left to view here)</div>
                        </div>
                      }
                    </ng-container>
                    <app-schema-diagram
                      *ngIf="readmeDiagramSegmentIndex === 1"
                      [schema$]="combinedSchema$"
                      [displayedMembers]="allMemberLinks"
                    ></app-schema-diagram>
                  </div>
                </div>
              </div>
            </as-split-area>
            <as-split-area *ngIf="selectedModel || selectedOperation" size="50">
              <app-schema-diagram
                [schema$]="combinedSchema$"
                [displayedMembers]="editable ? availableMemberLinks : [selectedModel ? selectedModel.name.fullyQualifiedName : selectedOperation.memberQualifiedName.fullyQualifiedName]"
                [memberNameNavigable]="!editable"
              ></app-schema-diagram>
            </as-split-area>
          </as-split>
        </as-split-area>
      </as-split>
      <app-code-viewer
        *ngIf="activeTabIndex === 1"
        class='code-editor'
        [sources]="versionedSources"
        [flexboxMode]="codeViewerFlexBoxMode"
      ></app-code-viewer>
    </div>
    <div class="error-message-box" *ngIf="saveResultMessage && saveResultMessage.severity === 'FAILURE'">
      {{ saveResultMessage.message }}
    </div>
    <div class="button-bar" *ngIf="editable">
      <button tuiButton appearance="secondary" size="m" (click)="cancelConfig.emit()" [loading]="working">
        Cancel
      </button>
      <button tuiButton size="m" (click)="savePendingEdits()" [loading]="working">Save</button>
    </div>
  `,
  styleUrls: ['./schema-member-type-explorer.component.scss'],
})
export class SchemaMemberTypeExplorerComponent  {

  activeTabIndex: number = 0;
  readmeDiagramSegmentIndex: number = 0

  selectedModel: Type;
  selectedOperation: ServiceMember;
  availableMemberLinks: string[];
  allMemberLinks: string[];
  private combinedSchema: Schema;

  // TODO: this should only be handling the error state - be good to align these around the TUI notification
  @Input()
  saveResultMessage: Message;

  readonly schema$ = new BehaviorSubject<Schema>(null);

  @Input() set schema(value: Schema) {
    this.schema$.next(value);
  }

  get schema(): Schema {
    return this.schema$.value;
  }

  @Input()
  readme: string

  @Input()
  isReadmeLoading: boolean

  @Input()
  working: boolean = false;

  @Input()
  editable: boolean = false;

  @Input()
  allowTryItOut: boolean = false;

  @Input()
  codeViewerFlexBoxMode: CodeViewerFlexBoxMode = 'flex'

  get versionedSources(): VersionedSource[] {
    if (!this._partialSchema) {
      return null;
    }
    const submission = this._partialSchema as SchemaSubmissionResult
    return submission.sourcePackage.sources;
  }

  readonly combinedSchema$: Observable<Schema>

  constructor(
    private detectorRef: ChangeDetectorRef,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private queryPanelStoreService: QueryPanelStoreService,
    markdownService: MarkdownService,
    customMarkdownRenderer: CustomMarkdownRenderer,
    viewContainerRef: ViewContainerRef,
    zone: NgZone
  ) {
    this.combinedSchema$ = combineLatest([
      this.schema$,
      this.partialSchema$.pipe(startWith({types: [], services: []} as PartialSchema))
    ]).pipe(
      filter(([schema, partialSchema]) => {
        return !this.editable || (this.editable && !!partialSchema.types.length && !!partialSchema.services.length)
      }),
      map(([schema, partialSchema]) => {
        this.combinedSchema = combineAndCloneWithPartialSchema(schema, partialSchema);
        if (this.editable) {
          console.log("coming from schema update")
          this.availableMemberLinks = this.getAvailableMemberLinks();
          this.detectorRef.markForCheck();
        }
        return this.combinedSchema;
      })
    )
    if (this.editable)
      this.clearQueryParams();

    Prism.languages.taxi = taxi
    Prism.languages.taxiql = taxi

    InlineRunQueryButtonComponent.install(viewContainerRef, ((code) => this.gotoQueryEditor(code)), zone)
    customMarkdownRenderer.installFor(markdownService);
  }

  get hasCodeView(): boolean {
    return this._partialSchema && "sourcePackage" in this._partialSchema;
  }

  private _partialSchema$: Observable<PartialSchema> = new ReplaySubject<PartialSchema>(1)
  @Input()
  get partialSchema$(): Observable<PartialSchema> {
    return this._partialSchema$;
  }

  set partialSchema$(value) {
    if (value) {
      this._partialSchema$ = value.pipe(
        tap(value => {
          this._partialSchema = value;
          this.allMemberLinks = this.getAllMemberLinks();
          this.detectorRef.markForCheck();
        })
      )
    }
  }

  private _partialSchema: PartialSchema;
  @Input()
  get partialSchema(): PartialSchema {
    return this._partialSchema;
  }

  set partialSchema(value) {
    if (this._partialSchema === value) {
      return;
    }
    this._partialSchema = value;
    if (value) {
      (this.partialSchema$ as ReplaySubject<PartialSchema>).next(value);
    }
  }

  pendingEdits: SchemaEditOperation[] = [];


  @Output()
  save = new EventEmitter<SchemaEditOperation[]>();

  @Output()
  cancelConfig = new EventEmitter<void>();

  onModelSelected($event: Type) {
    this.selectedModel = $event;
    this.selectedOperation = null;
    if (this.editable) {
      this.availableMemberLinks = this.getAvailableMemberLinks();
    }
    this.router.navigate([],
      {
        relativeTo: this.activatedRoute,
        queryParams: {
          'selectedMember': this.selectedModel.fullyQualifiedName
        },
        queryParamsHandling: "merge",
      },
    );
  }

  onOperationSelected($event: ServiceMember) {
    this.selectedModel = null;
    this.selectedOperation = $event;
    if (this.editable) {
      this.availableMemberLinks = this.getAvailableMemberLinks();
    }
    this.router.navigate([],
      {
        relativeTo: this.activatedRoute,
        queryParams: {
          'selectedMember': this.selectedOperation.memberQualifiedName.fullyQualifiedName
        },
        queryParamsHandling: "merge",
      },
    );
  }

  onResetMemberSelection() {
    this.selectedModel = null;
    this.selectedOperation = null;
  }

  // the schemaEditOperation is sent to the server, the updatedType and originalType are used to maintain state in the UI
  handleSchemaEditOperation(schemaEditOperation: SchemaEditOperation, updatedType: Type | Operation, originalType: Type | Operation) {
    this.pendingEdits.push(schemaEditOperation);
    // this persists the changes on the client, albeit a bit crudely
    Object.assign(originalType, updatedType)
    this.partialSchema = JSON.parse(JSON.stringify(this.partialSchema));
  }

  // NOTE: not used for now
  handleNewTypeCreated(newType: Type, selectedModel: Type) {
    // TODO : This approach won't work anymore.
    // We need to be emitting a subtype of SchemaEditOperation, which defines
    // the action
    // Next step: At this point we should be adding an edit to the pendingEdits array
    throw new Error('Not supported')
    // this.partialSchema.types.push(newType);
    // (this.partialSchema$ as ReplaySubject<PartialSchema>).next(this.partialSchema)
  }

  savePendingEdits() {
    const editsToSubmit = [];
    if ("pendingEdits" in this.partialSchema) {
      const submissionResult = this.partialSchema as SchemaSubmissionResult
      editsToSubmit.push(...submissionResult.pendingEdits)
    }
    editsToSubmit.push(...this.pendingEdits)
    this.save.next(editsToSubmit);
  }

  private getAvailableMemberLinks() {
    if (!this.combinedSchema) return;

    let selectedTypeName: string;
    let links: Links;
    if (this.selectedModel) {
      selectedTypeName = this.selectedModel.fullyQualifiedName;
      links = this.selectedModel.isScalar ?
        buildLinksForModelWithAttributes(this.selectedModel.memberQualifiedName, this.selectedModel.attributes, this.combinedSchema, this.combinedSchema.operations) :
        buildLinksForType(this.selectedModel.memberQualifiedName, this.combinedSchema, this.combinedSchema.operations, null);
    } else if (this.selectedOperation) {
      selectedTypeName = this.selectedOperation.memberQualifiedName.fullyQualifiedName;
      const serviceAssociatedWithOperation = findServiceAssociatedWithOperation(this.combinedSchema.services, this.selectedOperation);
      links = buildOperationLinks(this.selectedOperation, serviceAssociatedWithOperation, this.combinedSchema);
    }
    // Use a set here to make the array unique
    const availableMemberLinks = [...new Set(
      [selectedTypeName]
        .concat(links.inputs.map(input => input.sourceNodeName.fullyQualifiedName))
        .concat(links.outputs.map(output => output.targetNodeName.fullyQualifiedName)))
    ]
    console.log("availableLinkages", availableMemberLinks);
    return availableMemberLinks;
  }

  private getAllMemberLinks() {
    // return all the services and the models they return for the selected project
    // first get a list of the services from partialSchema
    const allMemberLinks = this.partialSchema.services.flatMap(service => {
      // then get the operations associated with that Service
      const operations = collectAllServiceOperations(service)
      return operations.flatMap(operation => {
        const serviceAssociatedWithOperation = findServiceAssociatedWithOperation(this.partialSchema.services, operation);
        const links = buildOperationLinks(operation, serviceAssociatedWithOperation, this.partialSchema as Schema);
        return [
          ...links.inputs.map(input => input.sourceNodeName.fullyQualifiedName),
          ...links.outputs.map(output => output.targetNodeName.fullyQualifiedName)
        ]
          .filter(parameterizedName => {
          // we don't want primitive types (ie. taxi.lang...) showing up
          try {
            return !findType(this.partialSchema as Schema, parameterizedName)?.isPrimitive
          } catch (e) {}
        })
          // don't forget to add the operation name in!
          .concat(operation.qualifiedName.fullyQualifiedName)
      })
    })
    console.log("allMemberLinks", [...new Set(allMemberLinks)]);
    return [...new Set(allMemberLinks)]
  }

  private clearQueryParams() {
    this.router.navigate([], {
      queryParams: {selectedMember: null},
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  private gotoQueryEditor(query) {
    this.queryPanelStoreService.addTab('', query)
    this.router.navigate(['/query/editor']);
  }

  protected readonly of = of;
  protected readonly UiCustomisations = UiCustomisations;
}
