import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import { Field, getDisplayName, Metadata, QualifiedName } from '../../services/schema';
import { TypeMemberTreeNode } from './model-member.component';
import {
  AddOrRemoveFieldAnnotationEvent,
  ChangeFieldTypeEvent, EditMemberDescriptionEvent,
  SchemaEditOperation
} from '../../project-import/schema-importer.service';

@Component({
  selector: 'app-model-member-tree-node',
  template: `
    <div class="field-row">
      <span class="field-name">{{ treeNode.name }}: </span>
      <span
        [class.type-name-container]="editable"
        (click)="typeNameClicked.emit()"
        [attr.title]="editable ? 'Click to edit type association' : null"
      >
        <span class="mono-badge">
          <ng-container *ngIf="editable">{{ displayName(treeNode.type.name, showFullTypeNames) }}</ng-container>
          <ng-container *ngIf="!editable && !schemaMemberNavigable">{{ displayName(treeNode.type.name, showFullTypeNames) }}</ng-container>
          <a *ngIf="!editable && schemaMemberNavigable">{{ displayName(treeNode.type.name, showFullTypeNames) }}</a>
          <span class="scalar-base-type" [class.schema-member-navigable]="schemaMemberNavigable" *ngIf="treeNode.type.isScalar">
            {{ '(' + (treeNode.type.basePrimitiveTypeName?.shortDisplayName || displayName(treeNode.type.aliasForType, showFullTypeNames)) + ')' }}
          </span>
        </span>
        <img *ngIf="editable" src='assets/img/tabler/pencil.svg'>
      </span>
      <tui-tag class="new-tag" size="s" *ngIf="treeNode.isNew" value="New"></tui-tag>
      <span class="field-spacer"></span>
      @if (editable) {
       <label tuiLabel class="checkbox-label">
         <input tuiCheckbox type="checkbox" size="m"
                              [ngModel]="memberHasIdAnnotation"
                              (ngModelChange)="memberHasIdAnnotationChanged($event)"
        >Id
        </label>
       <label tuiLabel class="checkbox-label">
         <input tuiCheckbox type="checkbox" size="m"
                              [ngModel]="!treeNode.field.nullable"
                              (ngModelChange)="onRequiredChanged(treeNode.field)"
        >Required
        </label>
      } @else {
        <tui-badge
          *ngIf="memberHasIdAnnotation"
          appearance="info"
          size="s"
        >Id</tui-badge>
        <tui-badge
          *ngIf="!treeNode.field.nullable"
          appearance="warning"
          size="s"
        >Required</tui-badge>
      }
    </div>
    <div *ngIf="!!(treeNode.field.typeDoc || editable)" class="description-editor-container">
      <app-description-editor-container
        [type]="treeNode.field"
        [editable]="editable"
        [showHeader]="false"
        commitMode="explicit"
        (updateDeferred)="onDescriptionEdited()"
      ></app-description-editor-container>
    </div>
  `,
  styleUrls: ['./model-member-tree-node.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ModelMemberTreeNodeComponent implements OnInit {

  @Input()
  treeNode: TypeMemberTreeNode;

  @Input()
  editable: boolean;

  @Input()
  schemaMemberNavigable: boolean;

  @Output()
  typeNameClicked = new EventEmitter()

  @Input()
  showFullTypeNames = false;

  @Output()
  nodeUpdated = new EventEmitter<SchemaEditOperation>();

  private currentDescription: string;

  constructor(private changeDetector: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.currentDescription = this.treeNode.field.typeDoc;
  }

  displayName(name: QualifiedName, showFullTypeNames: boolean): string {
    return getDisplayName(name, showFullTypeNames);
  }

  get memberHasIdAnnotation(): boolean {
    return (this.treeNode.field.metadata || []).some((element: Metadata) => {
      return element.name.fullyQualifiedName === 'Id'
    });
  }

  memberHasIdAnnotationChanged(value: boolean) {
    const event: AddOrRemoveFieldAnnotationEvent = {
      editKind: 'AddOrRemoveFieldAnnotation',
      symbol: this.treeNode.parentModel.memberQualifiedName,
      fieldName: this.treeNode.name,
      annotationName: 'Id',
      operation: this.memberHasIdAnnotation ? 'Remove' : 'Add'
    }
    if (value) {
      this.treeNode.field.metadata.push({
        name: QualifiedName.from('Id'),
        params: {}
      })
    } else {
      const index = this.treeNode.field.metadata.findIndex(element => element.name.fullyQualifiedName === 'Id');
      this.treeNode.field.metadata.splice(index, 1);
    }
    this.nodeUpdated.emit(event);
  }

  onRequiredChanged(field: Field) {
    field.nullable = !field.nullable;
    const event: ChangeFieldTypeEvent = {
      editKind: 'ChangeFieldType',
      symbol: this.treeNode.parentModel.memberQualifiedName,
      fieldName: this.treeNode.name,
      newReturnType: field.type,
      nullable: field.nullable
    };
    this.nodeUpdated.emit(event);
  }

  onDescriptionEdited() {
    if (this.currentDescription === this.treeNode.field.typeDoc) return;
    this.currentDescription = this.treeNode.field.typeDoc;
    const event: EditMemberDescriptionEvent = {
      editKind: 'EditMemberDescription',
      symbol: this.treeNode.parentModel.name,
      memberKind: 'TYPE',
      memberName: this.treeNode.name,
      typeDoc: this.treeNode.field.typeDoc,
    }
    this.nodeUpdated.emit(event);
  }
}
