import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {CaseCondition, Policy, PolicyStatement} from "./policies";

@Component({
  selector: 'app-statement-display',
  template: `
    <div class="component-container">
      <div *ngIf="caseCondition" class="statement-line">
        <span>When caller's</span>
        <span class="editable-content" (click)="startEdit()">{{ caseCondition.description() }}</span>
        <span>then</span>
        <span class="editable-content" (click)="startEdit()">{{ statement.instruction.description() }}</span>
      </div>
      <div *ngIf="!caseCondition" class="statement-line">
        <span *ngIf="policy.rules[0].statements.length > 1">Otherwise, </span>
        <span class="editable-content" (click)="startEdit()">{{statement.instruction.description()}}</span>
      </div>
    </div>
  `,
  styleUrls: ['./statement-display.component.scss']
})
export class StatementDisplayComponent implements OnInit {

  ngOnInit() {
  }

  @Input()
  policy: Policy;

  @Input()
  statement: PolicyStatement;

  @Output()
  edit: EventEmitter<void> = new EventEmitter();

  get caseCondition(): CaseCondition {
    if (!this.statement || !this.statement.condition) return null;
    if (this.statement.condition.text !== 'case') return null;
    return <CaseCondition>this.statement.condition;
  }

  startEdit() {
    this.edit.emit()
  }
}
