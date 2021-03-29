import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {QueryHistoryModule} from './query-history.module';
import {ResponseStatus, VyneQlQueryHistorySummary} from '../services/query.service';
import {RouterTestingModule} from '@angular/router/testing';

storiesOf('Query History', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, QueryHistoryModule, RouterTestingModule]
    })
  ).add('Query cards', () => {
  return {
    template: `<div style="padding: 40px; width: 100%;">
<app-query-list [historyRecords]="historyRecords"></app-query-list>
</div>`,
    props: {
      historyRecords: [
        {
          durationMs: 20300,
          query: 'findAll { foo.bar.baz }',
          queryId: '123',
          recordSize: 2300,
          responseStatus: ResponseStatus.COMPLETED,
          timestamp: new Date()
        },
        {
          durationMs: 200300,
          query: 'findAll { foo.bar.baz }',
          queryId: '123',
          recordSize: 2300,
          responseStatus: ResponseStatus.COMPLETED,
          timestamp: new Date()
        }, {
          durationMs: 2300,
          query: 'findAll { foo.bar.baz }',
          queryId: '123',
          recordSize: 0,
          responseStatus: ResponseStatus.ERROR,
          timestamp: new Date()
        },
      ] as VyneQlQueryHistorySummary[]
    }
  };
});
