import {Component, Input, OnInit} from '@angular/core';
import {Documented, QualifiedName, Schema} from '../services/schema';

@Component({
  selector: 'app-type-editor',
  templateUrl: './type-editor.component.html',
  styleUrls: ['./type-editor.component.scss']
})
export class TypeEditorComponent {

  @Input()
  schema: Schema;

  spec: NewTypeSpec = new NewTypeSpec();

}

class NewTypeSpec implements Documented {
  namespace: string;
  typeName: string;
  inheritsFrom: QualifiedName;
  typeDoc: string;
}
