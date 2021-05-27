import {Component, EventEmitter, Input, Output} from '@angular/core';
import {QualityReport, RuleGrade, RuleGradedEvaluation} from './quality.service';
import {DatePipe} from '@angular/common';
import {gradeClassName, iconFor} from './quality-score.component';

@Component({
  selector: 'app-quality-card',
  templateUrl: './quality-card.component.html',
  styleUrls: ['./quality-card.component.scss'],
})
export class QualityCardComponent {
  @Output()
  drillToRule = new EventEmitter<string>();

  @Input()
  get qualityReport(): QualityReport {
    return this._qualityReport;
  }

  set qualityReport(value: QualityReport) {
    if (this._qualityReport === value) {
      return;
    }
    this._qualityReport = value;
    this.scoresByDate = this.rebuildChartData();
  }

  private _qualityReport: QualityReport;

  @Input()
  ruleAttributePerformance: RuleGradedEvaluation;

  scoresByDate: any[];
  view: any[] = [700, 300];

  // options
  showLabels = true;
  animations = true;
  xAxis = true;
  yAxis = true;
  showYAxisLabel = false;
  showXAxisLabel = false;
  xAxisLabel = 'Date';
  yAxisLabel = 'Score';
  timeline = false;

  colorScheme = {
    domain: ['#5AA454', '#E44D25', '#CFC0BB', '#7aa3e5', '#a8385d', '#aae3f5']
  };

  private rebuildChartData(): any[] {
    const values = this.qualityReport.averagedScoreByDate.map(dateReport => {
      return {
        name: dateReport.date,
        value: dateReport.score
      };
    });

    const result = [{
      name: 'Overall data quality',
      series: values
    }];
    console.log(JSON.stringify(result));
    return result;
  }

  getGradeClassName(grade: RuleGrade): string {
    return gradeClassName(grade);
  }

  getIconFor(grade: RuleGrade) {
    return iconFor(grade);
  }
}
