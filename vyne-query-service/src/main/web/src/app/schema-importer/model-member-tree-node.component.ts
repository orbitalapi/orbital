import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Type} from '../services/schema';
import {TypeMemberTreeNode} from './model-member.component';

@Component({
  selector: 'app-model-member-tree-node',
  template: `
    <div class="field-row">
      <span class="field-name">{{ treeNode.name }}</span>
      <span class="field-spacer">•</span>
      <span *ngIf="!editable">{{ treeNode.type.name.shortDisplayName }}</span>
      <button tuiLink [pseudo]="true" (click)="showTypeSelector()" *ngIf="editable">{{ treeNode.type.name.shortDisplayName }}</button>
      <span *ngIf="treeNode.type.isScalar"> ({{ treeNode.type.basePrimitiveTypeName?.shortDisplayName || treeNode.type.aliasForType?.shortDisplayName}})</span>
      <tui-tag size="s" *ngIf="treeNode.isNew" value="New"></tui-tag>
    </div>
    <div class="toggle-buttons-row">
      <tui-checkbox-labeled [size]="'m'"
                            [ngModel]="treeNode.field.nullable"
              (click)="(editable) ? treeNode.field.nullable = !treeNode.field.nullable : null;"
                            >Required</tui-checkbox-labeled>
      <tui-checkbox-labeled [size]="'m'"
                            (click)="treeNode.field.nullable = !treeNode.field.nullable"
                            >Id</tui-checkbox-labeled>
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

  @Output()
  editTypeRequested = new EventEmitter()

  startEditingDescription() {
    if (!this.editable) {
      return;
    }
    this.treeNode.editingDescription = true;
  }

  showTypeSelector() {
    this.editTypeRequested.emit();
  }
}
