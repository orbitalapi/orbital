import {MatDialog, MatDialogRef} from '@angular/material/dialog';
import {Input} from '@angular/core';
import {Schema, Type} from '../../services/schema';
import {TypeSearchContainerComponent} from '../type-search/type-search-container.component';

export class BaseSchemaMemberDisplay {


  constructor(protected dialog: MatDialog) {
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

export function openTypeSearch(dialog: MatDialog): MatDialogRef<TypeSearchContainerComponent, Type> {
  return dialog.open(TypeSearchContainerComponent, {
    height: '80vh',
    width: '1600px',
    maxWidth: '80vw'
  });
}
