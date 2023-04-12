import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
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
