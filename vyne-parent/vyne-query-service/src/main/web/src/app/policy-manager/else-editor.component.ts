import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {CaseCondition, PolicyStatement} from "./policies";
import {Schema} from "../services/types.service";

@Component({
  selector: 'app-else-editor',
  template: `
    <div class="else-editor-container">
      <div class="line-wrapper">
        <span>Otherwise,&nbsp;</span>
        <app-instruction-selector [instruction]="statement?.instruction" (statementUpdated)="statementUpdated.emit()"></app-instruction-selector>
      </div>
    </div>
  `,
  styleUrls: ['./else-editor.component.scss']
})
export class ElseEditorComponent {

  @Input()
  statement: PolicyStatement;

  @Output()
  statementUpdated: EventEmitter<string> = new EventEmitter();

}
