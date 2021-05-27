import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {QualityCardModule} from './quality-card.module';
import {AveragedScoreByDate, QualityReport, RuleGrade, RuleGradedEvaluation} from './quality.service';


const qualityReport: QualityReport = {
  overallScore: 75,
  numberOfEvents: 230,
  overallGrade: RuleGrade.WARNING,
  averagedScoreByDate: dateData(),
  averagedScoreBySubject: [],
  ruleSummaries: [
    {ruleName: 'Value failed to parse', recordCount: 256, score: 25, grade: RuleGrade.BAD},
    {ruleName: `Field 'foo' was null`, recordCount: 76, score: 67, grade: RuleGrade.WARNING},
    {ruleName: 'Unknown enum value', recordCount: 150, score: 90, grade: RuleGrade.GOOD},
  ]
};

const emptyReport: QualityReport = {
  ...qualityReport,
  numberOfEvents: 0,
};

const rulePerformance: RuleGradedEvaluation = {
  ruleName: 'Value failed to parse',
  evaluations: [
    {path: 'emailAddress', grade: RuleGrade.GOOD, score: 90, recordCount: 30},
    {path: 'firstName', grade: RuleGrade.BAD, score: 25, recordCount: 332},
    {path: 'emailAddress', grade: RuleGrade.GOOD, score: 25, recordCount: 30},
  ]
};

function dateData(): AveragedScoreByDate[] {
  const minimum = 65;
  const maximum = 98;
  const results: AveragedScoreByDate[] = [];
  for (let i = 30; i > 0; i--) {
    const score = Math.floor(Math.random() * (maximum - minimum + 1)) + minimum;
    const date = new Date();
    date.setDate(date.getDate() - i);
    results.push({
      date: date,
      score: score,
      recordCount: 890
    });
  }
  return results;


}

storiesOf('Quality score card', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, QualityCardModule]
    })
  ).add('Quality card', () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 250px; background-color: #f5f6fa" >
    <app-quality-card [qualityReport]="qualityReport" style="margin-bottom: 4rem; display: block"></app-quality-card>
    <app-quality-score [score]="25" [grade]="'BAD'"></app-quality-score>
    <app-quality-score [score]="75" [grade]="'WARNING'"></app-quality-score>
    <app-quality-score [score]="95" [grade]="'GOOD'"></app-quality-score>
    </div>
 `,
    props: {
      qualityReport
    }
  };
})
  .add('Empty state', () => {
    return {
      template: `<div style="padding: 40px; width: 100%; height: 250px; background-color: #f5f6fa" >
    <app-quality-card [qualityReport]="qualityReport" style="margin-bottom: 4rem; display: block"></app-quality-card>
    </div>
 `,
      props: {
        qualityReport: emptyReport
      }
    };
  })
  .add('Attribute drill-down', () => {
    return {
      template: `<div style="padding: 40px; width: 100%; height: 250px; background-color: #f5f6fa" >
    <app-quality-card [qualityReport]="qualityReport"
    [ruleAttributePerformance]="rulePerformance"
    style="margin-bottom: 4rem; display: block"></app-quality-card>
    </div>
 `,
      props: {
        rulePerformance: rulePerformance,
        qualityReport: qualityReport
      }
    };
  });
