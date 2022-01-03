import {Component, Input} from '@angular/core';
import {Type} from '../../services/schema';
import {isNullOrUndefined} from 'util';
import {MatDialog} from '@angular/material/dialog';
import {BaseSchemaMemberDisplay, openTypeSearch} from './base-schema-member-display';

@Component({
  selector: 'app-model-attribute-tree-list',
  template: `
    <app-model-member *ngFor="let field of model.attributes | keyvalue"
                      [member]="field.value"
                      [memberName]="field.key"
                      [editable]="editable"
                      [new]="true"
                      [anonymousTypes]="anonymousTypes"
                      [schema]="schema"></app-model-member>
  `,
  styleUrls: ['./model-attribute-tree-list.scss']
})
export class ModelAttributeTreeListComponent extends BaseSchemaMemberDisplay {

  constructor(dialog: MatDialog) {
    super(dialog);
  }

  @Input()
  model: Type;

  get isModel(): Boolean {
    return this.model && Object.keys(this.model.attributes).length > 0;
  }


  setInheritedType() {
    const dialog = openTypeSearch(this.dialog);
    dialog.afterClosed().subscribe((result) => {
      if (!isNullOrUndefined(result)) {
        const resultType = result as Type;
        this.model.inheritsFrom = [resultType.name];
        this.model.basePrimitiveTypeName = resultType.basePrimitiveTypeName;
      }
    })
  }
}
