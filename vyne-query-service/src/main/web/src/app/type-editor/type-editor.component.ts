import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Schema, Type } from '../services/schema';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { debounceTime } from 'rxjs/operators';
import { NewTypeSpec } from 'src/app/type-editor/new-type-spec';

@Component({
  selector: 'app-type-editor',
  templateUrl: './type-editor.component.html',
  styleUrls: ['./type-editor.component.scss'],
})
export class TypeEditorComponent {

  typeSpecFormGroup = new FormGroup({
    namespace: new FormControl(),
    typeName: new FormControl(null, Validators.required),
    inheritsFrom: new FormControl(),
    typeDoc: new FormControl(),
  });

  constructor() {
    this.typeDocValueChanged = new EventEmitter<string>();
    this.typeDocValueChanged
      .pipe(
        debounceTime(500),
      )
      .subscribe(value => {
          this.typeSpecFormGroup.get('typeDoc').setValue(value);
        },
      );
  }

  @Input()
  schema: Schema;

  @Input()
  working = false;

  @Input()
  errorMessage: string | null = null;

  spec: NewTypeSpec = new NewTypeSpec();

  @Output()
  cancel = new EventEmitter();

  @Output()
  create = new EventEmitter<NewTypeSpec>();

  typeDocValueChanged: EventEmitter<string>;

  save() {
    console.log(this.typeSpecFormGroup.getRawValue());
    this.create.emit(this.typeSpecFormGroup.getRawValue() as NewTypeSpec);
  }

  inheritsFromChanged(type: Type) {
    this.typeSpecFormGroup.get('inheritsFrom').setValue(type.name);
  }
}

