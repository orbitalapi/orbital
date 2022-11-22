import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { QualifiedName, Schema, SchemaMember, Type, VersionedSource } from '../services/schema';
import { Contents } from './toc-host.directive';
import { environment } from '../../environments/environment';
import { Inheritable } from '../inheritence-graph/inheritance-graph.component';
import { OperationQueryResult } from '../services/types.service';
import { Router } from '@angular/router';
import { isNullOrUndefined } from 'util';
import { Observable } from 'rxjs';

/**
 * Whether changes should be saved immediately, or
 * on an explicit save event.
 *
 */
export type CommitMode = 'immediate' | 'explicit';

@Component({
  selector: 'app-type-viewer',
  templateUrl: './type-viewer.component.html',
  styleUrls: ['./type-viewer.component.scss'],
  // OnPush breaks the ngx-graph display.  Not sure why.
  // changeDetection: ChangeDetectionStrategy.OnPush
})
export class TypeViewerComponent {

  showPolicyManager: boolean;
  schemaMember: SchemaMember;

  chartDisplayMode: 'links' | 'lineage' = 'links';

  private _type: Type;

  @Input()
  schema: Schema;

  @Input()
  schema$: Observable<Schema>;

  @Input()
  showFullTypeNames = false;

  @Input()
  commitMode: CommitMode = 'immediate';

  private _editable = false;

  @Output()
  typeUpdated: EventEmitter<Type> = new EventEmitter<Type>();

  @Output()
  newTypeCreated = new EventEmitter<Type>()

  @Input()
  get editable(): boolean {
    return this._editable;
  }

  set editable(value: boolean) {
    this._editable = value;
    if (this.editable) {
      // When editable has been changed to true, update the type to
      // force taking a clone
      this.type = this._type;
    }
  }

  // Set this if we're viewing a type where the
  // attributes might not exist in the schema ye.
  // eg - when we're importing new types.
  @Input()
  anonymousTypes: Type[] = [];

  sources: VersionedSource[];

  sourceTaxi: string;

  @Input()
  showAttributes = true;

  @Input()
  showTags = true;

  @Input()
  showDocumentation = true;

  @Input()
  showUsages = true;

  @Input()
  showTaxi = true;

  @Input()
  showInheritanceGraph = true;

  @Input()
  inheritanceView: Inheritable;

  @Input()
  typeUsages: OperationQueryResult;

  constructor(private router: Router,
              private changeDetector: ChangeDetectorRef) {
    this.showPolicyManager = false; //environment.showPolicyManager;
  }

  @Input()
  showContentsList = true;

  /**
   * Sets the type.
   * Note - if this component is editable (or becomes editable),
   * then a copy of the type is taken and used, rather than the
   * explicit type, as making global mutations on shared types
   * would be bad.
   *
   * For updates to the type, subscribe to typeUpdated
   */
  @Input()
  get type(): Type {
    return this._type;
  }

  get requiredMembers(): string[] {
    return [this.type.name.fullyQualifiedName];
  }

  set type(value: Type) {
    this._type = value;
    if (this.type && this._editable) {
      // When editable, take a clone of the type.
      this._type = JSON.parse(JSON.stringify(this.type));
    }
    if (this.type) {
      this.schemaMember = SchemaMember.fromType(this.type);
      this.sources = this.schemaMember.sources || [];
      this.sourceTaxi = (this.sources).map(v => v.content)
        .join('\n');
    }
    this.changeDetector.markForCheck();
  }

  contents: Contents;

  get hasAttributes() {
    if (!this._type) {
      return false;
    }
    return this._type.attributes && Object.keys(this._type.attributes).length > 0;
  }

  get hasEnumValues() {
    if (!this._type) {
      return false;
    }
    return this._type.enumValues && Object.keys(this._type.enumValues).length > 0;
  }

  navigateToType($event: QualifiedName) {
    this.router.navigate(['/catalog', getTypeNameToView($event).fullyQualifiedName])
  }
}

export function getTypeNameToView(name: QualifiedName): QualifiedName {
  // TODO : We just assume this is an array here.  Let's fix that later.
  if (!isNullOrUndefined(name.parameters) && name.parameters.length > 0) {
    return name.parameters[0]
  } else {
    return name;
  }
}
