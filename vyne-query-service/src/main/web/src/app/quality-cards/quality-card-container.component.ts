import {Component, Input} from '@angular/core';
import {DataQualityService, Period, QualityReport, RuleGradedEvaluation} from './quality.service';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-quality-card-container',
  template: `
    <app-quality-card *ngIf="qualityReport" [qualityReport]="qualityReport"
                      (drillToRule)="loadRuleDetail($event)"
                      [ruleAttributePerformance]="ruleDetailEvaluation"></app-quality-card>
  `,
  styleUrls: ['./quality-card-container.component.scss']
})
export class QualityCardContainerComponent {
  private _qualitySubjectTypeName: string;
  ruleDetailEvaluation: RuleGradedEvaluation;

  @Input()
  get qualitySubjectTypeName(): string {
    return this._qualitySubjectTypeName;
  }

  set qualitySubjectTypeName(value: string) {
    if (this._qualitySubjectTypeName === value) {
      return;
    }
    this._qualitySubjectTypeName = value;
    this.loadData();
  }

  constructor(private qualityService: DataQualityService) {
  }

  qualityReport: QualityReport;
  period: Period = Period.Last30Days;

  private loadData() {
    if (isNullOrUndefined(this.qualitySubjectTypeName)) {
      return;
    }
    this.qualityReport = null;
    this.qualityService.loadQualityReport(this.qualitySubjectTypeName, this.period)
      .subscribe(qualityReport => this.qualityReport = qualityReport,
        error => {
          console.error(JSON.stringify(error));
        }
      );
  }

  loadRuleDetail(ruleName: string) {
    this.qualityService.loadRuleDetailReport(
      this.qualitySubjectTypeName, ruleName, this.period
    ).subscribe(report => this.ruleDetailEvaluation = report);
  }
}
