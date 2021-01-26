import {Component, EventEmitter, Input, Output} from '@angular/core';
import {BaseTypedInstanceViewer} from './BaseTypedInstanceViewer';
import {DownloadFileType} from '../query-panel/result-display/result-container.component';
import {
  findType,
  InstanceLike,
  InstanceLikeOrCollection,
  isTypedInstance,
  isTypeNamedInstance,
  Schema,
  Type
} from '../services/schema';
import {isArray, isNullOrUndefined} from 'util';

@Component({
  selector: 'app-object-view-container',
  template: `
    <div class="container">
      <div class="toolbar">
        <div class="type-name">{{ type?.name.shortDisplayName }}</div>
        <mat-button-toggle-group [(ngModel)]="displayMode">
          <mat-button-toggle value="table" aria-label="Text align left">
            <img class="icon" src="assets/img/table-view.svg">
          </mat-button-toggle>
          <mat-button-toggle value="tree" aria-label="Text align left">
            <img class="icon" src="assets/img/tree-view.svg">
          </mat-button-toggle>
        </mat-button-toggle-group>
        <div class="spacer"></div>
        <button mat-stroked-button [matMenuTriggerFor]="menu" *ngIf="downloadSupported"
                class="downloadFileButton">Download
        </button>
        <mat-menu #menu="matMenu">
          <button mat-menu-item (click)="onDownloadClicked(downloadFileType.JSON)">as JSON</button>
          <button mat-menu-item (click)="onDownloadClicked(downloadFileType.CSV)">as CSV</button>
        </mat-menu>
      </div>
      <div *ngIf="ready" class="display-wrapper">
        <div *ngIf="hasDrilledFields" class="drilled-fields-list">
          <span class="label">Drilled fields:</span>
          <span *ngFor="let drilledField of drilledFields; index as i" class="drilled-field"
                (click)="drillUpToIndex(i)">{{drilledField}}</span>
        </div>
        <app-results-table *ngIf="displayMode==='table'"
                           [instance]="instance"
                           [schema]="schema"
                           [selectable]="selectable"
                           [type]="type"
                           (drillToFieldClicked)="drilldownDataToField($event)"
                           (instanceClicked)="instanceClicked.emit($event)">
        </app-results-table>
        <app-object-view *ngIf="displayMode==='tree' && instance"
                         [instance]="instance"
                         [schema]="schema"
                         [selectable]="selectable"
                         [type]="type"
                         (instanceClicked)="instanceClicked.emit($event)">
        </app-object-view>
      </div>
    </div>
  `,
  styleUrls: ['./object-view-container.component.scss']
})
export class ObjectViewContainerComponent extends BaseTypedInstanceViewer {
  // workaroun for lack of enum support in templates
  downloadFileType = DownloadFileType;

  get displayMode(): DisplayMode {
    return this._displayMode;
  }

  set displayMode(value: DisplayMode) {
    this._displayMode = value;
  }

  @Input()
  drilledFields: string[] = [];


  get hasDrilledFields() {
    return this.drilledFields.length > 0;
  }

  private drilledInstance: InstanceLikeOrCollection;
  private drilledType: Type = null;

  private _displayMode: DisplayMode = 'table';
  @Input()
    // tslint:disable-next-line:no-inferrable-types
  selectable: boolean = false;

  get ready() {
    return this.instance && this.schema && this.type;
  }

  @Output()
  downloadClicked = new EventEmitter<DownloadClickedEvent>();

  @Input()
  get instance(): InstanceLikeOrCollection {
    return (this.drilledFields.length === 0) ? this._instance : this.drilledInstance;
  }

  set instance(value: InstanceLikeOrCollection) {
    this._instance = value;
    this._localInstance = value;
    this._localInstanceCopy = JSON.parse(JSON.stringify(value));
    // When the instance changes, any assumptions we've made about
    // types based on the old instance are invalid, so null out to recompute
    this._derivedType = null;
    this._collectionMemberType = null;
  }

  get type(): Type {
    return (this.drilledFields.length === 0) ? super.type : this.drilledType;
  }

  set type(value: Type) {
    super.setType(value);
  }

  @Input()
  downloadSupported = false;

  private _localInstance: InstanceLikeOrCollection;
  private _localInstanceCopy: InstanceLikeOrCollection;


  onDownloadClicked(format: DownloadFileType) {
    this.downloadClicked.emit(new DownloadClickedEvent(format));
  }

  drilldownDataToField(fieldName: string) {
    this.drilledFields.push(fieldName);

    function getDrilledType(parentType: Type, memberTypeName: string, schema: Schema): Type {
      const field = parentType.attributes[memberTypeName];
      if (isNullOrUndefined(field)) {
        console.error(`Cannot drill into field ${memberTypeName} on type ${parentType.name.fullyQualifiedName} as it does not exist`);
        return null;
      }
      const fieldTypeName = field.type;
      return findType(schema, fieldTypeName.parameterizedName);
      // Work around for collection types.
      // Returning the type of Array<T> here caused display errors as subsequent code tried to fetch properties
      // from Array, rather than T.
      // So, if we find an array type, use it's member, rather than the array type directly.
      // Could better encapsulate this check to isArrayType() or something in the future
      // if (fieldTypeName.parameters.length !== 0) {
      //   return findType(schema, fieldTypeName.parameters[0].fullyQualifiedName);
      // } else {
      //   return findType(schema, fieldTypeName.parameterizedName);
      // }

    }

    function drillInstance(instance: InstanceLikeOrCollection,
                           instanceFieldName: string,
                           instanceFieldType: Type): InstanceLikeOrCollection {
      if (isTypedInstance(instance) || isTypeNamedInstance(instance)) {
        if (Array.isArray(instance.value)) {
          return instance.value.map(collectionMember => {
            return drillInstance(collectionMember, instanceFieldName, instanceFieldType) as InstanceLike;
          });
        } else {
          const drilledValue = instance.value[instanceFieldName];
          return drilledValue as InstanceLikeOrCollection;
        }
      } else {
        return instance[instanceFieldName] as InstanceLikeOrCollection;
      }
    }


    let newDrilledType = super._type;
    let newDrilledInstance = super._instance;
    this.drilledFields.forEach(drilledFieldName => {
      newDrilledType = getDrilledType(newDrilledType, drilledFieldName, this.schema);
      newDrilledInstance = drillInstance(newDrilledInstance, drilledFieldName, newDrilledType);
    });

    this.drilledType = newDrilledType;
    this.drilledInstance = newDrilledInstance;
  }

  drillUpToIndex(index: number) {
    if (index === 0) {
      this.drilledFields = [];
    } else {
      // js trick which reduces the size of the array, dropping all elements after
      this.drilledFields.length = index;
    }
  }
}

export type DisplayMode = 'table' | 'tree';

export class DownloadClickedEvent {
  constructor(public readonly format: DownloadFileType) {
  }
}
