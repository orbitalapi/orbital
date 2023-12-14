import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {OperationKind, QualifiedName, ServiceKind, TypeKind} from "../services/schema";

@Component({
  selector: 'app-catalog-entry-line',
  template: `
      <div class="row">
          <img *ngIf="getIcon()" [src]="getIcon()">
          <span class="result-type-label" *ngIf="!getIcon()">{{getLabel()}}</span>
          <ng-container *ngIf="fieldName">
          <span class="element-label">{{fieldName}}
            <span class="attribute-model-name"
                  *ngIf="showModelNamesForFields"
            >(on {{qualifiedName.shortDisplayName}})</span></span>
          </ng-container>
        <ng-container *ngIf="!fieldName">
              <span class="element-label"
                    [tuiHint]="labelTooltip"
              >{{qualifiedName.shortDisplayName}}</span>
        </ng-container>
        <button *ngIf="showAddToQueryButton"
                class="add-to-query-button"
                tuiIconButton
                type="button"
                size="xs"
                appearance="icon"
                icon="tuiIconPlusCircle"
                (click)="addToQueryClicked.emit(qualifiedName)"
                [tuiHint]="'Add to query'"
        ></button>
        <span class="mono-badge small"
              *ngIf="primitiveType">{{primitiveType.shortDisplayName}}</span>
      </div>
      <ng-template #labelTooltip>
        <span class="tooltip">{{ qualifiedName.shortDisplayName }}</span>
      </ng-template>
  `,
  styleUrls: ['./catalog-entry-line.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CatalogEntryLineComponent {
  @Input()
  showModelNamesForFields: boolean = true;

  @Input()
  primitiveType: QualifiedName;

  @Input()
  qualifiedName: QualifiedName;

  @Input()
  fieldName: string;

  @Input()
  serviceOrTypeKind: TypeKind | ServiceKind | OperationKind;

  @Input()
  allowAddToQuery: boolean = true;

  @Output()
  addToQueryClicked = new EventEmitter<QualifiedName>();


  get showAddToQueryButton(): boolean {
    if (!this.allowAddToQuery) return false;
    return this.serviceOrTypeKind === "Type" || this.serviceOrTypeKind === "Model" || this.serviceOrTypeKind === "Table" || this.serviceOrTypeKind === "ApiCall" || this.serviceOrTypeKind === "Stream";
  }

  getLabel(): string | null {
    if (this.serviceOrTypeKind !== "Type") {
      return null;
    }
    if (this.fieldName) {
      return 'Attr'
    } else {
      return 'Type'
    }
  }

  getIcon(): string | null {
    switch (this.serviceOrTypeKind) {
      case "API":
        return 'assets/img/tabler/server.svg';
      case "ApiCall":
        return 'assets/img/tabler/settings-2.svg'
      case "Database":
        return 'assets/img/tabler/database.svg';
      case "Kafka":
        return 'assets/img/iconpark-icons/voice-message.svg';
      case "Table":
      case "Model":
      // case "Type":
        return 'assets/img/tabler/table.svg';
      case 'Query':
        return 'assets/img/tabler/brand-google-big-query.svg';
      // case "Model":
      //   return 'assets/img/tabler/table.svg'
    }
    return null;
  }

}
