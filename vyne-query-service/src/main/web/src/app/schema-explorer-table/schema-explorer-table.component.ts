import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { Message, Operation, PartialSchema, Schema, ServiceMember, Type } from 'src/app/services/schema';
import { Observable, ReplaySubject } from 'rxjs';
import { tap } from 'rxjs/operators';

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
          <div class="documentation-content">
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
          </div>
        </as-split-area>

      </as-split>

    </div>
    <div class="error-message-box" *ngIf="saveResultMessage && saveResultMessage.level === 'FAILURE'">
      {{saveResultMessage.message}}
    </div>
    <div class="button-bar" *ngIf="editable">
      <button tuiButton size="m" (click)="save.emit(partialSchema)" [showLoader]="working">Save</button>
      <tui-notification status="success" *ngIf="saveResultMessage && saveResultMessage.level === 'SUCCESS'">
        {{ saveResultMessage.message }}
      </tui-notification>
    </div>

  `,
  styleUrls: ['./schema-explorer-table.component.scss'],
})
export class SchemaExplorerTableComponent {

  constructor(private changeDetection: ChangeDetectorRef) {
  }

  selectedModel: Type;
  selectedOperation: ServiceMember;

  @Input()
  saveResultMessage: Message;

  @Input()
  schema: Schema;

  @Input()
  working: boolean = false;

  @Input()
  editable: boolean = true;

  @Input()
  allowTryItOut: boolean = false;


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


  @Output()
  save = new EventEmitter<PartialSchema>();

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
    Object.assign(originalType, updatedType)
  }

  handleNewTypeCreated(newType: Type, selectedModel: Type) {
    this.partialSchema.types.push(newType);
    (this.partialSchema$ as ReplaySubject<PartialSchema>).next(this.partialSchema)
  }
}
