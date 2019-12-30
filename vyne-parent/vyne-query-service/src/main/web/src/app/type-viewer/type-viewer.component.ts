import {Component, Input} from '@angular/core';
import {SchemaMember, Type} from '../services/schema';
import {Contents} from "./toc-host.directive";
import {Source} from "../code-viewer/code-viewer.component";

@Component({
  selector: 'app-type-viewer',
  templateUrl: './type-viewer.component.html',
  styleUrls: ['./type-viewer.component.scss']
})
export class TypeViewerComponent {
  schemaMember: SchemaMember;

  private _type: Type;

  sources: Source[];

  @Input()
  get type(): Type {
    return this._type;
  }

  set type(value: Type) {
    this._type = value;
    if (this.type) {
      this.schemaMember = SchemaMember.fromType(this.type);
      this.sources = this.schemaMember.sources.map(source => {
        return {
          name: `Published by ${source.origin}`,
          language: 'taxi',
          code: source.content
        } as Source;
      });
    }
  }


  description = 'A person who buys coffee, hopefully lots of it, and collects points like gollum collects shiney rings.  Filth';

  contents: Contents;

  get hasAttributes() {
    if (!this._type) {
      return false;
    }
    return this._type.attributes && Object.keys(this._type.attributes).length > 0;
  }


}
