import {Component, Input, OnInit} from '@angular/core';
import {RuleGrade} from './quality.service';

@Component({
  selector: 'app-quality-score',
  template: `
    <div class="score-container" [ngClass]="scoreContainerClass">
      <div class="score">{{score}}</div>
      <div class="grade">{{overallGrade}}</div>
    </div>
  `,
  styleUrls: ['./quality-card.component.scss']
})
export class QualityScoreComponent {

  @Input()
  score: number;

  @Input()
  grade: RuleGrade;

  get overallGrade(): string {
    if (!this.grade) {
      return null;
    }
    return gradeLabel(this.grade);
  }

  get scoreContainerClass(): string {
    if (!this.overallGrade) {
      return null;
    }
    return gradeClassName(this.grade);
  }

}

export function gradeLabel(grade: RuleGrade): string {
  switch (grade) {
    case RuleGrade.GOOD:
      return 'High';
    case RuleGrade.WARNING:
      return 'Medium';
    case RuleGrade.BAD:
      return 'Low';
  }
}

export function iconFor(grade: RuleGrade) {
  switch (grade) {
    case RuleGrade.GOOD:
      return 'check_circle';
    case RuleGrade.WARNING:
      return 'warning';
    case RuleGrade.BAD:
      return 'stop';
  }
}

export function gradeClassName(grade: RuleGrade): string {
  return gradeLabel(grade).toLowerCase();
}
