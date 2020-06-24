import {Component, Input} from '@angular/core';
import {Schema, SchemaMember, SourceCode, Type, VersionedSource} from '../services/schema';
import {Contents} from './toc-host.directive';
import {environment} from '../../environments/environment';
import {buildInheritable, Inheritable} from '../inheritence-graph/inheritance-graph.component';

@Component({
  selector: 'app-type-viewer',
  templateUrl: './type-viewer.component.html',
  styleUrls: ['./type-viewer.component.scss']
})
export class TypeViewerComponent {
  showPolicyManager: boolean;
  schemaMember: SchemaMember;

  private _type: Type;

  @Input()
  schema: Schema;

  sources: VersionedSource[];

  @Input()
  inheritanceView: Inheritable;

  constructor() {
    this.showPolicyManager = environment.showPolicyManager;
  }


  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    this._type = value;
    if (this.type) {
      this.schemaMember = SchemaMember.fromType(this.type);
      this.sources = this.schemaMember.sources;
    }
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
}
