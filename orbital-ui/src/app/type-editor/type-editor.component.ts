import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Schema, Type } from '../services/schema';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { debounceTime } from 'rxjs/operators';
import { NewTypeSpec } from 'src/app/type-editor/new-type-spec';

@Component({
  selector: 'app-type-editor',
  templateUrl: './type-editor.component.html',
  styleUrls: ['./type-editor.component.scss'],
})
export class TypeEditorComponent {

  typeSpecFormGroup = new UntypedFormGroup({
    namespace: new UntypedFormControl(),
    typeName: new UntypedFormControl(null, Validators.required),
    inheritsFrom: new UntypedFormControl(),
    typeDoc: new UntypedFormControl(),
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

