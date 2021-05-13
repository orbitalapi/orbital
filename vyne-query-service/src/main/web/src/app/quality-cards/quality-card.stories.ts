import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {QualityCardModule} from './quality-card.module';
import {AveragedScoreByDate, QualityReport, RuleGrade} from './quality.service';


const qualityReport: QualityReport = {
  overallScore: 75,
  overallGrade: RuleGrade.WARNING,
  averageByDate: dateData(),
  ruleSummaries: [
    {title: 'Value failed to parse', count: 256, score: 25, grade: RuleGrade.BAD},
    {title: `Field 'foo' was null`, count: 76, score: 67, grade: RuleGrade.WARNING},
    {title: 'Unknown enum value', count: 150, score: 90, grade: RuleGrade.GOOD},
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
});
