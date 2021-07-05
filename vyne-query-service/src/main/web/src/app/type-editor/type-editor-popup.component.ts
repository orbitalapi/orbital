import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema} from '../services/schema';
import {NewTypeSpec} from './type-editor.component';
import {MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-type-editor-popup',
  template: `
    <h2>Create a new type</h2>
    <app-type-editor [schema]="schema" (cancel)="dialogRef.close(null)" (create)="dialogRef.close($event)"></app-type-editor>
  `,
  styleUrls: ['./type-editor-popup.component.scss']
})
export class TypeEditorPopupComponent {

  constructor(public dialogRef: MatDialogRef<TypeEditorPopupComponent>) {
  }

  @Input()
  schema: Schema;

}
