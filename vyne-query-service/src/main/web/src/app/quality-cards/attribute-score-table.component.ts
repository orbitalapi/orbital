import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {RuleGrade, RuleGradedEvaluation} from './quality.service';
import {gradeClassName, iconFor} from './quality-score.component';

@Component({
  selector: 'app-attribute-score-table',
  template: `
    <div class="subheader">
        <button mat-icon-button (click)="back.emit()"><mat-icon class="small-icon">arrow_back</mat-icon></button>
      <span class="subheader-text">{{ ruleAttributePerformance.ruleName }}</span>
    </div>
    <table class="summary-table">
      <thead>
      <th></th>
      <th>Attribute</th>
      <th>Score</th>
      <th>Count</th>
      </thead>
      <tbody>
      <tr *ngFor="let insightRow of ruleAttributePerformance.evaluations">
        <td>
          <mat-icon class="small-icon"
                    [ngClass]="getGradeClassName(insightRow.grade)">{{ getIconFor(insightRow.grade) }}</mat-icon>
        </td>
        <td>{{ insightRow.path }}</td>
        <td>{{ insightRow.score }}</td>
        <td>{{insightRow.recordCount}}</td>
      </tr>
      </tbody>
    </table>
  `,
  styleUrls: ['./quality-card.component.scss']
})
export class AttributeScoreTableComponent {


  @Input()
  ruleAttributePerformance: RuleGradedEvaluation;

  @Output()
  back = new EventEmitter();

  getGradeClassName(grade: RuleGrade): string {
    return gradeClassName(grade);
  }

  getIconFor(grade: RuleGrade) {
    return iconFor(grade);
  }
}
