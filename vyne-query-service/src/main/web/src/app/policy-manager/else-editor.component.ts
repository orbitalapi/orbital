import {Component, EventEmitter, Input, Output} from '@angular/core';
import {PolicyStatement, RuleSet, RuleSetUtils} from './policies';
import {Type} from '../services/schema';

@Component({
  selector: 'app-else-editor',
  template: `
    <div class="else-editor-container">
      <div class="line-wrapper">
        <span class="prefix-word">{{elsePrefixWord}}</span>
        <app-instruction-selector [statement]="statement" [policyType]="policyType"
                                  (statementUpdated)="statementUpdated.emit()"></app-instruction-selector>
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

  @Input()
  ruleSet: RuleSet;

  @Input()
  policyType: Type;

  get elsePrefixWord(): string {
    return RuleSetUtils.elsePrefixWord(this.ruleSet);
  }

}
