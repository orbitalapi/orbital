import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from '@angular/core';
import {Message, Operation, PartialSchema, Schema, ServiceMember, Type} from 'src/app/services/schema';
import {Observable, ReplaySubject} from 'rxjs';
import {tap} from 'rxjs/operators';
import {IMPORT_RESULT} from "../schema-importer/schema-code-preview/sample";
import {SchemaSubmissionResult} from "../services/types.service";
import {SchemaEdit, SchemaEditOperation} from "../schema-importer/schema-importer.service";

@Component({
  selector: 'app-schema-explorer-table',
  template: `
    <div class="main-content">
      <as-split direction="horizontal" unit="pixel">
        <as-split-area size="250">
          <app-schema-entry-table [partialSchema$]="partialSchema$" #schemaEntryTable
                                  (modelSelected)="onModelSelected($event)"
                                  (operationSelected)="onOperationSelected($event)"
          ></app-schema-entry-table>
        </as-split-area>
        <as-split-area size="*">
          <div class="documentation-content-container" *ngIf="hasCodeView">
            <div tuiGroup [collapsed]="true" class="radio-bar-container">
              <tui-radio-block size="s" [hideRadio]="true" item="docs" [(ngModel)]="displayMode">
                Documentation
              </tui-radio-block>
              <tui-radio-block size="s" [hideRadio]="true" item="code" [(ngModel)]="displayMode">Code
              </tui-radio-block>
            </div>

            <div class="documentation-content" *ngIf="displayMode === 'docs'">
              <app-type-viewer *ngIf="selectedModel"
                               [type]="selectedModel"
                               [schema]="schema"
                               [showUsages]="false"
                               [showContentsList]="false"
                               [anonymousTypes]="partialSchema?.types"
                               commitMode="explicit"
                               (newTypeCreated)="handleNewTypeCreated($event,selectedModel)"
                               (typeUpdated)="handleTypeUpdated($event,selectedModel)"
                               [editable]="editable"></app-type-viewer>
              <app-operation-view *ngIf="selectedOperation"
                                  [operation]="selectedOperation"
                                  [schema]="schema"
                                  [allowTryItOut]="allowTryItOut"
                                  [editable]="editable"
                                  (newTypeCreated)="handleNewTypeCreated($event,selectedOperation)"
                                  (updateDeferred)="handleTypeUpdated($event,selectedOperation)"
              ></app-operation-view>
              <div *ngIf="!selectedModel && !selectedOperation">
                Select a schema member from the panel on the left to view here.
              </div>
            </div>
            <app-code-editor
              *ngIf="displayMode === 'code'"
              class='code-editor'
              readOnly
              [content]='taxi' #editor></app-code-editor>
          </div>

        </as-split-area>

      </as-split>

    </div>
    <div class="error-message-box" *ngIf="saveResultMessage && saveResultMessage.level === 'FAILURE'">
      {{saveResultMessage.message}}
    </div>
    <div class="button-bar" *ngIf="editable">
      <button tuiButton size="m" (click)="savePendingEdits()" [showLoader]="working">Save</button>
      <tui-notification status="success" *ngIf="saveResultMessage && saveResultMessage.level === 'SUCCESS'">
        {{ saveResultMessage.message }}
      </tui-notification>
    </div>

  `,
  styleUrls: ['./schema-explorer-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchemaExplorerTableComponent {

  displayMode: 'docs' | 'code' = 'docs';

  selectedModel: Type;
  selectedOperation: ServiceMember;

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

  get taxi(): string {
    if (!this._partialSchema) {
      return null;
    }
    const submission = this._partialSchema as SchemaSubmissionResult
    return submission.sourcePackage.sources.map(s => s.content)
      .join('\n')
  }


  private _partialSchema$: Observable<PartialSchema> = new ReplaySubject<PartialSchema>(1)

  constructor(private changeDetection: ChangeDetectorRef) {
  }

  get hasCodeView(): boolean {
    return this._partialSchema && "taxi" in this._partialSchema;
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

  // TODO : We need to add edits to this as the user makes changes
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

  /**
   * When the type is updated in one of the editors, we swap out the definition
   * in the schemaSubmissionResult
   */
  handleTypeUpdated(updatedType: Type | Operation, originalType: Type | Operation) {
    // TODO : This approach won't work anymore.
    // We need to be emitting a subtype of SchemaEditOperation, which defines
    // the action
    // Next step: At this point we should be adding an edit to the pendingEdits array
    throw new Error('Not supported')

    // Object.assign(originalType, updatedType)
  }

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
