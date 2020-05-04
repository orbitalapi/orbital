import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {CaseCondition, Policy, PolicyStatement, RelativeSubject, RuleSetUtils} from './policies';

@Component({
  selector: 'app-statement-display',
  template: `
    <div class="component-container">
      <div *ngIf="caseCondition" class="statement-line">
        <span>When</span>
        <span class="editable-content" (click)="startEdit()"
              *ngIf="caseConditionDescription">{{ caseConditionDescription }}</span>
        <span class="editable-content hint" (click)="startEdit()" *ngIf="!caseConditionDescription">Click to edit</span>
        <span>then</span>
        <span class="editable-content" (click)="startEdit()">{{ statement.instruction.description() }}</span>
      </div>
      <div *ngIf="!caseCondition" class="statement-line">
        <span class="prefix-word">{{elsePrefixWord}}</span>
        <span class="editable-content" (click)="startEdit()">{{statement.instruction.description()}}</span>
      </div>
    </div>
  `,
  styleUrls: ['./statement-display.component.scss']
})
export class StatementDisplayComponent implements OnInit {

  ngOnInit() {
  }

  get elsePrefixWord(): string {
    return RuleSetUtils.elsePrefixWord(this.policy.ruleSets[0]);
  }

  @Input()
  readonly: boolean;

  @Input()
  policy: Policy;

  @Input()
  statement: PolicyStatement;

  @Output()
  edit: EventEmitter<void> = new EventEmitter();

  get caseCondition(): CaseCondition {
    if (!this.statement || !this.statement.condition) { return null; }
    if (this.statement.condition.type !== 'case') { return null; }
    return <CaseCondition>this.statement.condition;
  }

  get caseConditionDescription(): string {
    if (!this.caseCondition || !this.caseCondition.description()) { return null; }
    return this.caseCondition.description().replace(RelativeSubject.TYPE_TOKEN, this.policy.targetTypeName.name);
  }

  startEdit() {
    if (!this.readonly) {
      this.edit.emit();
    }

  }
}
