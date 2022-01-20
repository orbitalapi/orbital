import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema} from '../services/schema';
import {SchemaSubmissionResult, TypesService} from '../services/types.service';
import {NewTypeSpec} from './type-editor.component';
import {generateTaxi} from './taxi-generator';
import {MatSnackBar} from '@angular/material/snack-bar';
import {HttpErrorResponse} from '@angular/common/http';

@Component({
  selector: 'app-type-editor-container',
  template: `
    <app-type-editor [schema]="schema" (cancel)="cancel.emit()" (create)="saveType($event)"
                     [errorMessage]="errorMessage" [working]="working"></app-type-editor>
  `,
  styleUrls: ['./type-editor-container.component.scss']
})
export class TypeEditorContainerComponent {

  constructor(private schemaService: TypesService, private snackBar: MatSnackBar) {
    schemaService.getTypes()
      .subscribe(schema => this.schema = schema);
  }

  schema: Schema;

  @Output()
  cancel = new EventEmitter();

  errorMessage: string;
  working = false;

  @Output()
  typeCreated = new EventEmitter<SchemaSubmissionResult>();

  saveType(spec: NewTypeSpec) {
    this.working = true;
    this.errorMessage = null;
    const taxi = generateTaxi(spec);
    console.log(taxi);
    this.schemaService.submitTaxi(taxi)
      .subscribe((result: SchemaSubmissionResult) => {
          this.snackBar.open('Type saved successfully', 'Dismiss', {duration: 3000});
          this.typeCreated.emit(result);
          this.working = false;
        },
        (error: HttpErrorResponse) => {
          this.working = false;
          this.errorMessage = error.error.message;
        }
      );
  }
}
