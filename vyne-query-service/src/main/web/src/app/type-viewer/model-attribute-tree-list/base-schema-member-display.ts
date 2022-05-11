import {MatDialog, MatDialogRef} from '@angular/material/dialog';
import {Directive, Input} from '@angular/core';
import {Schema, Type} from '../../services/schema';
import {TypeSearchContainerComponent, TypeSelectedEvent} from '../type-search/type-search-container.component';
import {BaseDeferredEditComponent} from '../base-deferred-edit.component';

// Need this since we're inherits multiple layers deep.
// See https://github.com/angular/angular/issues/35295
@Directive()
export abstract class BaseSchemaMemberDisplay extends BaseDeferredEditComponent<Type> {


  constructor(protected dialog: MatDialog) {
    super();
  }

  @Input()
  editable: boolean = false;

  @Input()
  schema: Schema;

  @Input()
  anonymousTypes: Type[] = [];

  editingDescription: boolean = false;

  startEditingDescription() {
    if (!this.editable) {
      return;
    }
    this.editingDescription = true;
  }
}

export function openTypeSearch(dialog: MatDialog): MatDialogRef<TypeSearchContainerComponent, TypeSelectedEvent> {
  return dialog.open(TypeSearchContainerComponent, {
    height: '80vh',
    width: '1600px',
    maxWidth: '80vw'
  });
}
