import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Type} from '../services/schema';
import {openTypeSearch} from './model-attribute-tree-list/base-schema-member-display';
import {isNullOrUndefined} from 'util';
import {MatDialog} from '@angular/material/dialog';
import {BaseDeferredEditComponent} from './base-deferred-edit.component';

@Component({
  selector: 'app-inherits-from',
  template: `
    <div
      *ngIf="type.inheritsFrom.length > 0 && type.inheritsFrom[0].fullyQualifiedName !== type.basePrimitiveTypeName.fullyQualifiedName">
      <span>inherits&nbsp;</span>
      <ng-container *ngIf="!editable || type.inheritsFrom.length > 1">
        <div class="inherited-member" *ngFor="let inherits of type.inheritsFrom">
          <span class="mono-badge">{{inherits.longDisplayName}}</span>
          <span>({{type.basePrimitiveTypeName.shortDisplayName}})</span>
        </div>
      </ng-container>
      <div class="editable-type" *ngIf="editable && type.inheritsFrom.length === 1">
        <span class="mono-badge">{{ type.inheritsFrom[0].longDisplayName }}
        </span>
        <span>&nbsp;({{type.basePrimitiveTypeName.shortDisplayName}})</span>
        <mat-icon (click)="setInheritedType()">edit</mat-icon>
      </div>
    </div>
    <!-- when the type inherits directly from a primitive -->
    <div class="editable-type"
         *ngIf="type.inheritsFrom.length === 1 && type.inheritsFrom[0].fullyQualifiedName === type.basePrimitiveTypeName.fullyQualifiedName">
      <span>inherits&nbsp;</span>
      <span class="mono-badge">{{ type.basePrimitiveTypeName.shortDisplayName }}</span>
      <mat-icon (click)="setInheritedType()" *ngIf="editable">edit</mat-icon>

    </div>
  `,
  styleUrls: ['./inherits-from.component.scss']
})
export class InheritsFromComponent extends BaseDeferredEditComponent<Type>{

  constructor(private dialog: MatDialog) {
    super();
  }

  @Input()
  type: Type;

  @Input()
  editable: boolean;

  setInheritedType() {
    const dialog = openTypeSearch(this.dialog);
    dialog.afterClosed().subscribe((result) => {
      if (!isNullOrUndefined(result)) {
        const resultType = result as Type;
        this.type.inheritsFrom = [resultType.name];
        this.type.basePrimitiveTypeName = resultType.basePrimitiveTypeName;
        this.emitUpdateIfRequired();
      }
    })
  }
}
