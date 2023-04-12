import {Component, EventEmitter, Inject, Output} from '@angular/core';
import {SchemaTreeNode} from "../services/types.service";
import {TUI_TREE_LOADER, TUI_TREE_LOADING, TUI_TREE_START, TuiTreeService} from "@taiga-ui/kit";
import {SchemaTreeLoader} from "./schema-tree-loader";
import {TuiHandler} from "@taiga-ui/cdk";
import {OperationKind, QualifiedName, ServiceKind, TypeKind} from "../services/schema";

export const ROOT_NODE = {rootNode: 'Data sources'}

@Component({
  selector: 'app-catalog-tree',
  template: `
      <tui-tree
              [tuiTreeController]="false"
              [map]="map"
              [value]="service.data$ | async"
              [childrenHandler]="childrenHandler"
              [content]="content"
              (toggled)="onToggled($event)"
      ></tui-tree>
      <ng-template
              #content
              let-item
      >
          <tui-loader
            *ngIf="item === loading; else text"
            class="loader"
          ></tui-loader>
        <ng-template #text>
          <app-catalog-entry-line *ngIf="!isRootNode(item)"
                                  [qualifiedName]="n(item).element"
                                  [fieldName]="n(item).fieldName"
                                  [primitiveType]="n(item).primitiveType"
                                  [serviceOrTypeKind]="serviceOrType(n(item))"
                                  [showModelNamesForFields]="false"
                                  (addToQueryClicked)="addToQueryClicked.emit($event)"
                                  (click)="itemClicked.emit(n(item).element)"
          ></app-catalog-entry-line>
          <span *ngIf="isRootNode(item)" class="root-node">{{item.rootNode}}</span>
        </ng-template>
      </ng-template>
  `,
  providers: [
    TuiTreeService,
    {
      provide: TUI_TREE_START,
      useValue: ROOT_NODE,
    },
    {
      provide: TUI_TREE_LOADER,
      useClass: SchemaTreeLoader,
    },
  ],
  styleUrls: ['./catalog-tree.component.scss']
})
export class CatalogTreeComponent {

  @Output()
  itemClicked = new EventEmitter<QualifiedName>();

  @Output()
  addToQueryClicked = new EventEmitter<QualifiedName>();


  // to add typing into template
  n(item: any): SchemaTreeNode {
    return item as SchemaTreeNode;
  }

  isRootNode(item): boolean {
    return item === ROOT_NODE;
  }

  serviceOrType(item: SchemaTreeNode): ServiceKind | TypeKind | OperationKind {
    return item.typeKind || item.serviceKind || item.operationKind;
  }

  constructor(@Inject(TUI_TREE_LOADING) readonly loading: unknown,
              @Inject(TuiTreeService) readonly service: TuiTreeService<SchemaTreeNode>,
  ) {
    // this.service.loadChildren(ROOT_NODE as any)
  }

  map = new Map<any, boolean>(
    // [[ROOT_NODE, true]]
  );

  childrenHandler: TuiHandler<SchemaTreeNode, readonly SchemaTreeNode[]> = item => this.service.getChildren(item);

  onToggled(item: SchemaTreeNode): void {
    this.service.loadChildren(item);
  }
}
