import {Component, Input, OnInit} from '@angular/core';
import {Type} from '../services/schema';
import {TypeMemberTreeNode} from './model-member.component';

@Component({
  selector: 'app-model-member-tree-node',
  template: `
    <div class="field-row">
      <span class="field-name">{{ treeNode.name }}</span>
      <span class="field-spacer">•</span>
      <span>{{ treeNode.type.name.shortDisplayName }}</span>
      <span *ngIf="treeNode.type.isScalar"> ({{ treeNode.type.basePrimitiveTypeName?.shortDisplayName || treeNode.type.aliasForType?.shortDisplayName}})</span>
      <tui-tag size="s" *ngIf="treeNode.isNew" value="New"></tui-tag>
    </div>
    <div class="toggle-buttons-row">
      <button tuiButton [disabled]="!editable"
              appearance="whiteblock"
              (click)="treeNode.field.nullable = !treeNode.field.nullable"
              size="xs">{{ treeNode.field.nullable ? 'Required' : 'Optional' }}</button>
    </div>
    <div>
      <div *ngIf="!treeNode.editingDescription" (click)="startEditingDescription()"
           [ngClass]="{ 'editable-description' : editable}">
        <p *ngIf="treeNode.field.typeDoc">{{ treeNode.field.typeDoc }}</p>
        <span *ngIf="!treeNode.field.typeDoc" class="">No documentation here yet.</span><span
        *ngIf="editable && !treeNode.field.typeDoc">&nbsp; Click to add some.</span>
      </div>

      <tui-text-area *ngIf="treeNode.editingDescription"
                     class="description-editor"
                     [expandable]="true"
                     [rows]="2"
                     [(ngModel)]="treeNode.field.typeDoc"
                     [tuiTextfieldCleaner]="true"
                     (focusout)="treeNode.editingDescription = false"
      >
      </tui-text-area>
    </div>
  `,
  styleUrls: ['./model-member-tree-node.component.scss']
})
export class ModelMemberTreeNodeComponent {

  @Input()
  treeNode: TypeMemberTreeNode;

  @Input()
  editable: boolean;

  startEditingDescription() {
    if (!this.editable) {
      return;
    }
    this.treeNode.editingDescription = true;
  }
}
