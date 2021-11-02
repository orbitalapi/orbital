import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Documented, QualifiedName, Schema, Type} from '../services/schema';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ContentSupplier} from '../type-viewer/description-editor/description-editor.react';
import {pipe} from 'rxjs';
import {debounce, debounceTime} from 'rxjs/operators';
import {generateTaxi} from './taxi-generator';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-type-editor',
  templateUrl: './type-editor.component.html',
  styleUrls: ['./type-editor.component.scss']
})
export class TypeEditorComponent {

  typeSpecFormGroup = new FormGroup({
    namespace: new FormControl(),
    typeName: new FormControl(null, Validators.required),
    inheritsFrom: new FormControl(),
    typeDoc: new FormControl()
  });

  constructor() {
    this.typeDocValueChanged = new EventEmitter<ContentSupplier>();
    this.typeDocValueChanged
      .pipe(
        debounceTime(500)
      )
      .subscribe(next => {
          this.typeSpecFormGroup.get('typeDoc').setValue(next());
        }
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

  typeDocValueChanged: EventEmitter<ContentSupplier>;

  save() {
    console.log(this.typeSpecFormGroup.getRawValue());
    this.create.emit(this.typeSpecFormGroup.getRawValue() as NewTypeSpec);
  }

  inheritsFromChanged(type: Type) {
    this.typeSpecFormGroup.get('inheritsFrom').setValue(type.name);
  }
}

export class NewTypeSpec implements Documented {
  namespace: string | null;
  typeName: string;
  inheritsFrom: QualifiedName | null;
  typeDoc: string | null;

  qualifiedName(): QualifiedName {
    if (isNullOrUndefined(this.namespace)) {
      return QualifiedName.from(this.typeName);
    } else {
      return QualifiedName.from(`${this.namespace}.${this.typeName}`);
    }
  }
}
