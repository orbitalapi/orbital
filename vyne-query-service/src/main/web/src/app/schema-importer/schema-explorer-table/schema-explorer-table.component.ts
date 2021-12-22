import {Component, Input, OnInit} from '@angular/core';
import {SchemaSubmissionResult} from '../../services/types.service';
import {Schema, Type} from '../../services/schema';

@Component({
  selector: 'app-schema-explorer-table',
  template: `
    <app-schema-member-list [importedSchema]="schemaSubmissionResult"
                            (modelSelected)="onModelSelected($event)"
    ></app-schema-member-list>
    <app-model-display *ngIf="selectedModel"
                       [model]="selectedModel" [editable]="true"
                       [anonymousTypes]="schemaSubmissionResult?.types"
                       [schema]="schema"></app-model-display>
  `,
  styleUrls: ['./schema-explorer-table.component.scss']
})
export class SchemaExplorerTableComponent {

  selectedModel: Type;

  @Input()
  schema: Schema;

  @Input()
  schemaSubmissionResult: SchemaSubmissionResult;

  onModelSelected($event: Type) {
    this.selectedModel = $event;
  }
}
