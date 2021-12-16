import {Component, Input, OnInit} from '@angular/core';
import {Schema, Type} from '../services/schema';

@Component({
  selector: 'app-model-display',
  template: `
    <h2>{{ model.name.fullyQualifiedName }}</h2>
    <h3>Attributes</h3>
    <app-model-member *ngFor="let field of model.attributes | keyvalue"
                      [member]="field.value"
                      [memberName]="field.key"
                      [editable]="editable"
                      [new]="true"
                      [schema]="schema"></app-model-member>
  `,
  styleUrls: ['./model-display.component.scss']
})
export class ModelDisplayComponent {

  @Input()
  editable: boolean = false;

  @Input()
  model: Type;

  @Input()
  schema: Schema;

}
