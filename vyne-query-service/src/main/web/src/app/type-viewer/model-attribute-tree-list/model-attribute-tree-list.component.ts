import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { QualifiedName, Type } from '../../services/schema';
import { isNullOrUndefined } from 'util';
import { MatDialog } from '@angular/material/dialog';
import { BaseSchemaMemberDisplay, openTypeSearch } from './base-schema-member-display';

@Component({
  selector: 'app-model-attribute-tree-list',
  template: `
    <app-model-member *ngFor="let field of model.attributes | keyvalue"
                      [member]="field.value"
                      [memberName]="field.key"
                      [editable]="editable"
                      [new]="true"
                      [showFullTypeNames]="showFullTypeNames"
                      [anonymousTypes]="anonymousTypes"
                      [commitMode]="commitMode"
                      (newTypeCreated)="newTypeCreated.emit($event)"
                      (updateDeferred)="this.updateDeferred.emit(type)"
                      (typeNameClicked)="typeNameClicked.emit($event)"
                      [schema]="schema"></app-model-member>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./model-attribute-tree-list.scss']
})
export class ModelAttributeTreeListComponent extends BaseSchemaMemberDisplay {

  constructor(dialog: MatDialog) {
    super(dialog);
  }

  @Input()
  showFullTypeNames = false;

  @Output()
  typeNameClicked = new EventEmitter<QualifiedName>()

  private _model: Type;
  @Input()
  get model(): Type {
    return this._model;
  }

  set model(value: Type) {
    this._model = value;
  }


  @Output()
  newTypeCreated = new EventEmitter<Type>()

  get type(): Type {
    return this._model;
  }

  get isModel(): Boolean {
    return this._model && Object.keys(this._model.attributes).length > 0;
  }


  setInheritedType() {
    const dialog = openTypeSearch(this.dialog);
    dialog.afterClosed().subscribe((result) => {
      if (!isNullOrUndefined(result)) {
        const resultType = result.type as Type;
        this._model.inheritsFrom = [resultType.name];
        this._model.basePrimitiveTypeName = resultType.basePrimitiveTypeName;
        if (result.source === 'new') {
          this.newTypeCreated.emit(resultType);
        }
      }
    })
  }
}
