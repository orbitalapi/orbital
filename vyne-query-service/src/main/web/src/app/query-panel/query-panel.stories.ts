import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {ResultsTableModule} from '../results-table/results-table.module';
import {QueryPanelModule} from './query-panel.module';

storiesOf('Query panel', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, QueryPanelModule]
    })
  ).add('Query editor', () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 250px" >
    <query-editor></query-editor>
    </div>`,
    props: {}
  };
})
  .add('Bottom bar states', () => {
    return {
      template: `<div style="padding: 40px; width: 100%; height: 250px" >
        <app-query-editor-bottom-bar currentState="Editing"></app-query-editor-bottom-bar>
        <app-query-editor-bottom-bar currentState="Running" [queryStarted]="queryStartDate"></app-query-editor-bottom-bar>
        <app-query-editor-bottom-bar currentState="Running" [queryStarted]="aMinuteAgo"></app-query-editor-bottom-bar>
        <app-query-editor-bottom-bar currentState="Error" error="A query failed to execute."></app-query-editor-bottom-bar>
    </div>`,
      props: {
        queryStartDate: new Date(),
        aMinuteAgo: new Date(new Date().getTime() - (1000 * 60))
      }
    };
  });
