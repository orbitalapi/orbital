import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import {
  collectAllServiceOperations,
  Operation,
  PartialSchema,
  Service,
  ServiceMember,
  Type
} from 'src/app/services/schema';

export interface TreeEntry {
  label: string;
  member: Type | Service | Operation
  category: 'type' | 'service' | 'operation' | 'model';
}

@Component({
  selector: 'app-schema-member-tree',
  template: `
    <ng-container [tuiTreeController]="true" *ngIf="schemaReceived">
      <tui-tree-item class="root-tree-item">
        <img src="assets/img/tabler/category-filled.svg" class="schema-member-icon filter-schema-model-color">
        <span class="schema-member-kind-label" [attr.data-kind]="models.length">Models</span>
        <tui-tree-item
          *ngFor="let entry of models"
          class="tree-item-leaf-node"
          [class.active]="entry.member.fullyQualifiedName === selectedMember"
          (click)="onModelSelected(entry)"
        >
          <img src="assets/img/tabler/file-description.svg">{{ entry.label }}
        </tui-tree-item>
      </tui-tree-item>
      <tui-tree-item class="root-tree-item">
        <img src="assets/img/tabler/category-filled.svg" class="schema-member-icon filter-schema-type-color">
        <span class="schema-member-kind-label" [attr.data-kind]="types.length">Types</span>
        <tui-tree-item
          *ngFor="let entry of types"
          class="tree-item-leaf-node"
          [class.active]="entry.member.fullyQualifiedName === selectedMember"
          (click)="onModelSelected(entry)"
        >
          <img src="assets/img/tabler/file-description.svg">{{ entry.label }}
        </tui-tree-item>
      </tui-tree-item>
      <tui-tree-item class="root-tree-item">
        <img src="assets/img/tabler/category-filled.svg" class="schema-member-icon filter-schema-service-color">
        <span class="schema-member-kind-label" [attr.data-kind]="services.length">Services</span>
        <tui-tree-item *ngFor="let service of services" class="root-tree-item">{{ service.label }}
          <tui-tree-item
            *ngFor="let operation of collectAllServiceOperations(service.member)"
            class="tree-item-leaf-node"
            [class.active]="operation.memberQualifiedName.fullyQualifiedName === selectedMember"
            (click)="onOperationSelected(operation)"
          >
            <img src="assets/img/tabler/file-description.svg">{{ operation.qualifiedName.shortDisplayName }}
          </tui-tree-item>
        </tui-tree-item>
      </tui-tree-item>
    </ng-container>
  `,
  styleUrls: ['./schema-member-tree.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SchemaMemberTreeComponent implements OnInit {

  constructor(
    private changeDetector: ChangeDetectorRef,
    private destroyRef: DestroyRef,
    private activatedRoute: ActivatedRoute
  ) {
  }

  ngOnInit(): void {
    this.activatedRoute.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(
        params => {
          this.selectedMember = params['selectedMember'];
          this.changeDetector.markForCheck();
          this.activateSelectedMember();
        }
      )
  }

  private _importedSchema: Observable<PartialSchema>;

  types: TreeEntry[];
  models: TreeEntry[];
  services: TreeEntry[];

  schemaReceived = false;
  selectedMember = '';

  @Output()
  modelSelected = new EventEmitter<Type>()

  @Output()
  operationSelected = new EventEmitter<ServiceMember>()

  @Output()
  resetSelection = new EventEmitter<void>()

  @Input()
  get partialSchema$(): Observable<PartialSchema> {
    return this._importedSchema;
  }

  set partialSchema$(value) {
    if (this.partialSchema$ === value) {
      return;
    }
    this._importedSchema = value;
    this.schemaReceived = false;
    this.partialSchema$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(next => {
        this.schemaReceived = true;
        const [types, models, services] = this.buildTree(next);
        this.types = types;
        this.models = models;
        this.services = services;
        this.activateSelectedMember();
        this.changeDetector.markForCheck();
      })
  }

  private buildTree(schema: PartialSchema): [TreeEntry[], TreeEntry[], TreeEntry[]] {
    function typeToEntry(type: Type): TreeEntry {
      return {
        category: 'type',
        label: type.name.shortDisplayName,
        member: type
      }
    }

    const types = schema
      .types
      .filter(type => type.isScalar)
      .map(type => typeToEntry(type))
      .sort((a, b) => a.label.localeCompare(b.label));
    const models = schema
      .types
      .filter(type => !type.isScalar)
      .map(type => typeToEntry(type))
      .sort((a, b) => a.label.localeCompare(b.label));
    const services = schema
      .services
      .map(service => {
        return {
          category: 'service',
          label: service.name.shortDisplayName,
          member: service
        } as TreeEntry
      })
      .sort((a, b) => a.label.localeCompare(b.label));

    return [types, models, services]
  }

  private activateSelectedMember() {
    if (!this.schemaReceived) return;
    // check to make sure the entry isn't already selected, if not, dispatch the modelSelected event
    const typeOrModelSelected = this.types.concat(this.models).find(item => item.member.memberQualifiedName.fullyQualifiedName === this.selectedMember);
    if (typeOrModelSelected !== undefined) {
      this.onModelSelected(typeOrModelSelected)
    }
    const operationSelected = this.services.flatMap(item => collectAllServiceOperations(item.member as Service))
      .find(item => item.memberQualifiedName.fullyQualifiedName === this.selectedMember);
    if (operationSelected !== undefined) {
      this.onOperationSelected(operationSelected)
    }

    if (!typeOrModelSelected && !operationSelected) {
      this.resetSelection.emit();
    }
  }

  onModelSelected(entry: TreeEntry) {
    this.modelSelected.emit(entry.member as Type);
  }

  onOperationSelected(operation: ServiceMember) {
    this.operationSelected.emit(operation)
  }

  protected readonly collectAllServiceOperations = collectAllServiceOperations;
}
