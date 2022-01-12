import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {SchemaSubmissionResult} from '../../services/types.service';
import {Message, Operation, Schema, ServiceMember, Type} from '../../services/schema';
import {SchemaEntryTableComponent} from './schema-entry-table.component';
import {Observable} from 'rxjs/internal/Observable';
import {ReplaySubject} from 'rxjs/index';

@Component({
  selector: 'app-schema-explorer-table',
  template: `
    <div class="main-content">
      <app-schema-entry-table [importedSchema$]="schemaBeingEdited$" #schemaEntryTable
                              (modelSelected)="onModelSelected($event)"
                              (operationSelected)="onOperationSelected($event)"
      ></app-schema-entry-table>
      <div class="documentation-content">
        <app-type-viewer *ngIf="selectedModel"
                         [type]="selectedModel"
                         [schema]="schema"
                         [showUsages]="false"
                         [showContentsList]="false"
                         [anonymousTypes]="_schemaSubmissionResult?.types"
                         commitMode="explicit"
                         (newTypeCreated)="handleNewTypeCreated($event,selectedModel)"
                         (typeUpdated)="handleTypeUpdated($event,selectedModel)"
                         [editable]="true"></app-type-viewer>
        <app-operation-view *ngIf="selectedOperation"
                            [operation]="selectedOperation"
                            [schema]="schema"
        ></app-operation-view>
      </div>
    </div>
    <div class="error-message-box" *ngIf="saveResultMessage && saveResultMessage.level === 'FAILURE'">
      {{saveResultMessage.message}}
    </div>
    <div class="button-bar">
      <button tuiButton size="m" (click)="save.emit(_schemaSubmissionResult)" [showLoader]="working">Save</button>
      <tui-notification status="success" *ngIf="saveResultMessage && saveResultMessage.level === 'SUCCESS'">
        {{ saveResultMessage.message }}
      </tui-notification>
    </div>

  `,
  styleUrls: ['./schema-explorer-table.component.scss']
})
export class SchemaExplorerTableComponent {

  selectedModel: Type;
  selectedOperation: ServiceMember;

  @Input()
  saveResultMessage: Message;

  @Input()
  schema: Schema;

  @Input()
  working: boolean = false;

  schemaBeingEdited$ = new ReplaySubject<SchemaSubmissionResult>(1)

  private _schemaSubmissionResult: SchemaSubmissionResult;
  @Input()
  get schemaSubmissionResult(): SchemaSubmissionResult {
    return this._schemaSubmissionResult;
  }

  set schemaSubmissionResult(value: SchemaSubmissionResult) {
    if (this.schemaSubmissionResult === value) {
      return;
    }
    this._schemaSubmissionResult = value;
    if (value) {
      this.schemaBeingEdited$.next(value);
    }
  }


  @Output()
  save = new EventEmitter<SchemaSubmissionResult>();

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
  handleTypeUpdated(updatedType: Type, originalType: Type) {
    Object.assign(originalType, updatedType)
  }

  handleNewTypeCreated(newType: Type, selectedModel: Type) {
    this.schemaSubmissionResult.types.push(newType);
    this.schemaBeingEdited$.next(this.schemaSubmissionResult)
  }
}
