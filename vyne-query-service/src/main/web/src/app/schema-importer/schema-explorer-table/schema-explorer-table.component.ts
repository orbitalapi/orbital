import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {SchemaSubmissionResult} from '../../services/types.service';
import {Operation, Schema, ServiceMember, Type} from '../../services/schema';

@Component({
  selector: 'app-schema-explorer-table',
  template: `
    <div class="main-content">
      <app-schema-entry-table [importedSchema]="schemaSubmissionResult"
                              (modelSelected)="onModelSelected($event)"
                              (operationSelected)="onOperationSelected($event)"
      ></app-schema-entry-table>
      <div class="documentation-content">
        <app-type-viewer *ngIf="selectedModel"
                         [type]="selectedModel"
                         [schema]="schema"
                         [showUsages]="false"
                         [showContentsList]="false"
                         [anonymousTypes]="schemaSubmissionResult?.types"
                         [editable]="true"></app-type-viewer>
        <app-operation-view *ngIf="selectedOperation"
                            [operation]="selectedOperation"
                            [schema]="schema"
        ></app-operation-view>
      </div>
    </div>
    <div class="button-bar">
      <button tuiButton size="m" (click)="save.emit(schemaSubmissionResult)">Save</button>
    </div>

  `,
  styleUrls: ['./schema-explorer-table.component.scss']
})
export class SchemaExplorerTableComponent {

  selectedModel: Type;
  selectedOperation: ServiceMember;

  @Input()
  schema: Schema;

  @Input()
  schemaSubmissionResult: SchemaSubmissionResult;

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
}
