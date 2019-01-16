import {Component, Input, OnInit} from '@angular/core';
import {CaseCondition, Instruction, Policy, PolicyStatement, RuleSet} from "./policies";

@Component({
  selector: 'app-policy-editor',
  templateUrl: './policy-editor.component.html',
  styleUrls: ['./policy-editor.component.scss']
})
export class PolicyEditorComponent implements OnInit {

  @Input()
  policy: Policy;

  constructor() {
  }

  ngOnInit() {
  }

  addCase(ruleset: RuleSet) {
    ruleset.appendStatement(new PolicyStatement(new CaseCondition(), Instruction.permit()))
  }

  onStatementUpdated() {
    console.log(this.policy.src())
  }

  deleteStatement(ruleset: RuleSet, $event: PolicyStatement) {
    ruleset.removeStatement($event)
  }
}
