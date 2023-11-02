import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {Type, TypeNamedInstance} from "../services/schema";

@Component({
  selector: 'app-type-named-instance-tree',
  styleUrls: ['./type-named-instance-tree.component.scss'],
  template: `
    <!--    <tui-tree-->
    <!--        [value]="treeDataPages[treeDataCurrentPage]"-->
    <!--        [tuiTreeController]="true"-->
    <!--        [content]="treeContent"-->
    <!--        [childrenHandler]="treeChildrenHandler"></tui-tree>-->
    <!--    <ng-template #treeContent let-item>-->
    <!--      <div class="tree-node">-->
    <!--        <div *ngIf="treeNode(item).fieldName" class="field-name">{{treeNode(item).fieldName}}</div>-->
    <!--        <div class="field-value" [class.selectable]="selectable"-->
    <!--             (click)="onAttributeClicked(item)">{{treeNode(item).value}}</div>-->
    <!--      </div>-->
    <!--    </ng-template>-->
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TypeNamedInstanceTreeComponent {

  @Input()
  instance: TypeNamedInstance | TypeNamedInstance[]

  @Input()
  anonymousTypes: Type[]

  @Input()
  type: Type;


}
