import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema} from '../services/schema';
import {NewTypeSpec} from './type-editor.component';

@Component({
  selector: 'app-type-editor-card',
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Create a new type</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <app-type-editor [schema]="schema" (cancel)="cancel.emit()" (create)="create.emit($event)"></app-type-editor>
      </mat-card-content>
    </mat-card>
  `,
  styleUrls: ['./type-editor-card.component.scss']
})
export class TypeEditorCardComponent  {

  @Input()
  schema: Schema;

  @Output()
  cancel = new EventEmitter();

  @Output()
  create = new EventEmitter<NewTypeSpec>();
}
