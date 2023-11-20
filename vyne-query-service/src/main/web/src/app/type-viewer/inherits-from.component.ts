import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Type } from '../services/schema';
import { openTypeSearch } from './model-attribute-tree-list/base-schema-member-display';
import { isNullOrUndefined } from 'src/app/utils/utils';
import { MatLegacyDialog as MatDialog } from '@angular/material/legacy-dialog';
import { BaseDeferredEditComponent } from './base-deferred-edit.component';

@Component({
  selector: 'app-inherits-from',
  template: `
    <div
      *ngIf="!inheritsFromPrimitive && type.inheritsFrom.length > 0">
      <ng-container *ngIf="!editable || type.inheritsFrom.length > 1">
        <span>inherits&nbsp;</span>
        <div class="inherited-member" *ngFor="let inherits of type.inheritsFrom">
          <span class="mono-badge">{{inherits.longDisplayName}}</span>
          <span *ngIf="type.basePrimitiveTypeName">({{type.basePrimitiveTypeName.shortDisplayName}})</span>
        </div>
      </ng-container>
      <div class="editable-type" *ngIf="editable && type.inheritsFrom.length === 1">
        <span>inherits&nbsp;</span>
        <span class="mono-badge">{{ type.inheritsFrom[0].longDisplayName }}
        </span>
        <span *ngIf="type.basePrimitiveTypeName">&nbsp;({{type.basePrimitiveTypeName.shortDisplayName}})</span>
        <mat-icon (click)="setInheritedType()">edit</mat-icon>
      </div>
    </div>
    <!-- when the type inherits directly from a primitive -->
    <div class="editable-type"
         *ngIf="inheritsFromPrimitive">
      <span>inherits&nbsp;</span>
      <span class="mono-badge">{{ type.basePrimitiveTypeName.shortDisplayName }}</span>
      <mat-icon (click)="setInheritedType()" *ngIf="editable">edit</mat-icon>

    </div>
  `,
  styleUrls: ['./inherits-from.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InheritsFromComponent extends BaseDeferredEditComponent<Type> {

  constructor(private dialog: MatDialog) {
    super();
  }

  @Input()
  type: Type;

  @Input()
  editable: boolean;

  get inheritsFromPrimitive(): boolean {
    try {
      return this.type && this.type.inheritsFrom && this.type.basePrimitiveTypeName && this.type.inheritsFrom.length === 1 && this.type.inheritsFrom[0].fullyQualifiedName === this.type.basePrimitiveTypeName.fullyQualifiedName
    } catch (e) {
      console.error(e);
    }

  }

  setInheritedType() {
    const dialog = openTypeSearch(this.dialog);
    dialog.afterClosed().subscribe((result) => {
      if (!isNullOrUndefined(result)) {
        const resultType = result.type;
        this.type.inheritsFrom = [resultType.name];
        this.type.basePrimitiveTypeName = resultType.basePrimitiveTypeName;
        this.emitUpdateIfRequired();
      }
    })
  }
}
