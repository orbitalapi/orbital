import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {QualityRuleSummary, RuleGrade} from './quality.service';
import {gradeClassName, iconFor} from './quality-score.component';

@Component({
  selector: 'app-rule-score-table',
  template: `
    <table class="summary-table">
      <thead>
      <th></th>
      <th>Rule</th>
      <th>Score</th>
      <th>Count</th>
      </thead>
      <tbody>
      <tr *ngFor="let insightRow of ruleSummaries">
        <td>
          <mat-icon class="small-icon"
                    [ngClass]="getGradeClassName(insightRow.grade)">{{ getIconFor(insightRow.grade) }}</mat-icon>
        </td>
        <td><a href="javascript:void(0);"
               (click)="drillToRule.emit(insightRow.ruleName)">{{ insightRow.ruleName }}</a>
        </td>
        <td>{{ insightRow.score }}</td>
        <td>{{insightRow.recordCount}}</td>
      </tr>
      </tbody>
    </table>
  `,
  styleUrls: ['./quality-card.component.scss']
})
export class RuleScoreTableComponent {
  @Input()
  ruleSummaries: QualityRuleSummary[];

  @Output()
  drillToRule = new EventEmitter<string>();

  getGradeClassName(grade: RuleGrade): string {
    return gradeClassName(grade);
  }

  getIconFor(grade: RuleGrade) {
    return iconFor(grade);
  }

}
