import {Component, Input} from '@angular/core';
import {SchemaMember, SourceCode, Type, VersionedSource} from '../services/schema';
import {Contents} from './toc-host.directive';
import {environment} from "../../environments/environment";

@Component({
  selector: 'app-type-viewer',
  templateUrl: './type-viewer.component.html',
  styleUrls: ['./type-viewer.component.scss']
})
export class TypeViewerComponent {
  showPolicyManager
  schemaMember: SchemaMember;

  private _type: Type;

  sources: VersionedSource[];

  constructor() {
    this.showPolicyManager = environment.showPolicyManager
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


  description = ''; //'A person who buys coffee, hopefully lots of it, and collects points like gollum collects shiney rings.  Filth';

  contents: Contents;

  get hasAttributes() {
    if (!this._type) {
      return false;
    }
    return this._type.attributes && Object.keys(this._type.attributes).length > 0;
  }

}
