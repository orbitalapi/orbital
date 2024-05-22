import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema} from '../services/schema';
import {MatDialogRef} from '@angular/material/dialog';
import { NewTypeSpec } from 'src/app/type-editor/new-type-spec';

@Component({
  selector: 'app-type-editor-popup',
  template: `
    <h2>Create a new type</h2>
    <app-type-editor-container (cancel)="dialogRef.close(null)" (typeCreated)="dialogRef.close($event)"></app-type-editor-container>
  `,
  styleUrls: ['./type-editor-popup.component.scss']
})
export class TypeEditorPopupComponent {

  constructor(public dialogRef: MatDialogRef<TypeEditorPopupComponent>) {
  }

}
