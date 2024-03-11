import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import {
  Message,
  Operation,
  PartialSchema,
  Schema,
  ServiceMember,
  Type,
  VersionedSource
} from 'src/app/services/schema';
import { Observable, ReplaySubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { SchemaSubmissionResult } from '../services/types.service';
import { SchemaEditOperation } from '../project-import/schema-importer.service';
import { CodeViewerFlexBoxMode } from '../code-viewer/code-viewer.component';

@Component({
  selector: 'app-schema-explorer-table',
  template: `
    <ng-container *ngIf="useIslandContainer; else forms">
      <tui-island class="island">
        <ng-container *ngTemplateOutlet="forms"></ng-container>
      </tui-island>
    </ng-container>
    <ng-template #forms>
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
          <as-split-area size="250">
            <app-schema-entry-table [partialSchema$]="partialSchema$" #schemaEntryTable
                                    (modelSelected)="onModelSelected($event)"
                                    (operationSelected)="onOperationSelected($event)"
            ></app-schema-entry-table>
          </as-split-area>
          <as-split-area size="*">
            <div class="documentation-content-container">
              <div class="documentation-content">
                <app-type-viewer *ngIf="selectedModel"
                                 [type]="selectedModel"
                                 [schema]="schema"
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
                <div *ngIf="!selectedModel && !selectedOperation">
                  Select a schema member from the panel on the left to view here.
                </div>
              </div>
            </div>
          </as-split-area>
        </as-split>
        <app-code-viewer
          *ngIf="activeTabIndex === 1"
          class='code-editor'
          [sources]="versionedSources"
          [flexboxMode]="codeViewerFlexBoxMode"
        ></app-code-viewer>
      </div>
      <div class="error-message-box" *ngIf="saveResultMessage && saveResultMessage.level === 'FAILURE'">
        {{ saveResultMessage.message }}
      </div>
      <div class="button-bar" *ngIf="editable">
        <button tuiButton size="m" (click)="savePendingEdits()" [showLoader]="working">Save</button>
      </div>
    </ng-template>
  `,
  styleUrls: ['./schema-explorer-table.component.scss'],
})
export class SchemaExplorerTableComponent {

  activeTabIndex: number = 0;

  selectedModel: Type;
  selectedOperation: ServiceMember;

  // TODO: this should only be handling the error state - be good to align these around the TUI notification
  @Input()
  saveResultMessage: Message;

  @Input()
  schema: Schema;

  @Input()
  working: boolean = false;

  @Input()
  editable: boolean = false;

  @Input()
  allowTryItOut: boolean = false;

  @Input()
  codeViewerFlexBoxMode: CodeViewerFlexBoxMode = 'flex'

  @Input()
  useIslandContainer: boolean;

  get versionedSources(): VersionedSource[] {
    if (!this._partialSchema) {
      return null;
    }
    const submission = this._partialSchema as SchemaSubmissionResult
    return submission.sourcePackage.sources;
  }


  private _partialSchema$: Observable<PartialSchema> = new ReplaySubject<PartialSchema>(1)

  constructor(private changeDetection: ChangeDetectorRef) {
  }

  get hasCodeView(): boolean {
    return this._partialSchema && "sourcePackage" in this._partialSchema;
  }

  @Input()
  get partialSchema$(): Observable<PartialSchema> {
    return this._partialSchema$;
  }

  set partialSchema$(value) {
    if (value) {
      this._partialSchema$ = value.pipe(
        tap(value => {
          this._partialSchema = value;
          this.changeDetection.markForCheck();
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

  onModelSelected($event: Type) {
    this.selectedModel = $event;
    this.selectedOperation = null;
  }

  onOperationSelected($event: ServiceMember) {
    this.selectedModel = null;
    this.selectedOperation = $event;
  }

  // the schemaEditOperation is sent to the server, the updatedType and originalType are used to maintain state in the UI
  handleSchemaEditOperation(schemaEditOperation: SchemaEditOperation, updatedType: Type | Operation, originalType: Type | Operation) {
    this.pendingEdits.push(schemaEditOperation);
    // this persists the changes on the client, albeit a bit crudely
    Object.assign(originalType, updatedType)
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
}
