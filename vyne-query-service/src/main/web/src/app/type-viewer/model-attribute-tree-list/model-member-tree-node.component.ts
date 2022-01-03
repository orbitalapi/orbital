import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Metadata, QualifiedName} from '../../services/schema';
import {TypeMemberTreeNode} from './model-member.component';

@Component({
  selector: 'app-model-member-tree-node',
  template: `
    <div class="field-row">
      <span class="field-name">{{ treeNode.name }}</span>
      <span class="field-spacer">•</span>
      <span *ngIf="!editable">{{ treeNode.type.name.shortDisplayName }}</span>
      <button tuiLink [pseudo]="true" (click)="showTypeSelector()"
              *ngIf="editable">{{ treeNode.type.name.shortDisplayName }}</button>
      <!-- using string concat in the span since the intellij formatter keeps adding empty spaces -->
      <span class="scalar-base-type"
        *ngIf="treeNode.type.isScalar"> {{ '(' + (treeNode.type.basePrimitiveTypeName?.shortDisplayName || treeNode.type.aliasForType?.shortDisplayName) + ')'}}
      </span>
      <tui-tag size="s" *ngIf="treeNode.isNew" value="New"></tui-tag>
      <span class="field-spacer">•</span>
      <tui-checkbox-labeled [size]="'m'"
                            [ngModel]="treeNode.field.nullable"
                            (click)="(editable) ? treeNode.field.nullable = !treeNode.field.nullable : null;"
      >Required
      </tui-checkbox-labeled>
      <tui-checkbox-labeled [size]="'m'"
                            [(ngModel)]="memberHasIdAnnotation"
      >Id
      </tui-checkbox-labeled>
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

  get memberHasIdAnnotation(): boolean {
    return this.treeNode.type.metadata.some((element: Metadata) => {
      return element.name.fullyQualifiedName === 'Id'
    });
  }

  set memberHasIdAnnotation(value) {
    if (!this.editable) {
      return;
    }
    if (value === this.memberHasIdAnnotation) {
      return;
    }
    if (value) {
      this.treeNode.type.metadata.push({
        name: QualifiedName.from('Id'),
        params: {}
      })
    } else {
      const index = this.treeNode.type.metadata.findIndex(element => element.name.fullyQualifiedName === 'Id');
      this.treeNode.type.metadata.splice(index, 1);
    }
  }
}
