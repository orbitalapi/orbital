import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {QueryHistoryModule} from './query-history.module';
import {ResponseStatus, VyneQlQueryHistorySummary} from '../services/query.service';
import {RouterTestingModule} from '@angular/router/testing';
import {RunningQueryStatus} from '../services/active-queries-notification-service';

const activeQueries = new Map<string, RunningQueryStatus>();
activeQueries.set('123', {
  queryId: '123',
  vyneQlQuery: 'findAll { foo.bar.baz }',
  completedProjections: 3,
  estimatedProjectionCount: 8,
  startTime: new Date(),
  running: true,
  responseTypeName: 'com.foo.Bar'
});
activeQueries.set('456', {
  queryId: '456',
  vyneQlQuery: 'findAll { foo.bar.baz }',
  completedProjections: 0,
  startTime: new Date(),
  estimatedProjectionCount: null,
  running: true,
  responseTypeName: 'com.foo.Bar'
});

storiesOf('Query History', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, QueryHistoryModule, RouterTestingModule]
    })
  ).add('Query cards', () => {
  return {
    template: `<div style="padding: 40px; width: 100%;">
<app-query-list [historyRecords]="historyRecords" [activeQueries]="activeQueries"></app-query-list>
</div>`,
    props: {
      activeQueries: [
        {
          queryId: '123',
          vyneQlQuery: 'findAll { foo.bar.baz }',
          completedProjections: 3,
          estimatedProjectionCount: 8,
          startTime: new Date(),
          running: true
        },
        {
          queryId: '456',
          vyneQlQuery: 'findAll { foo.bar.baz }',
          completedProjections: 0,
          startTime: new Date(),
          running: true
        }
      ] as RunningQueryStatus[],
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
