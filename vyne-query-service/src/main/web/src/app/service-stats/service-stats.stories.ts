import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {RemoteOperationPerformanceStats, ResponseCodeGroup} from '../services/query.service';
import {RouterTestingModule} from '@angular/router/testing';
import {ServiceStatsModule} from './service-stats.module';

const operationStats: RemoteOperationPerformanceStats[] = [
  {
    operationQualifiedName: '',
    serviceName: 'Service',
    operationName: 'Get all Customers',
    callsInitiated: 34,
    averageTimeToFirstResponse: 7,
    totalWaitTime: 204,
    responseCodes: {
      [ResponseCodeGroup.HTTP_2XX]: 24,
      [ResponseCodeGroup.HTTP_3XX]: 0,
      [ResponseCodeGroup.HTTP_4XX]: 5,
      [ResponseCodeGroup.HTTP_5XX]: 5,
      [ResponseCodeGroup.UNKNOWN]: 0,
    }
  },
  {
    operationQualifiedName: '',
    serviceName: 'Service',
    operationName: 'Find customer name',
    callsInitiated: 82,
    averageTimeToFirstResponse: 2,
    totalWaitTime: 503,
    responseCodes: {
      [ResponseCodeGroup.HTTP_2XX]: 45,
      [ResponseCodeGroup.HTTP_3XX]: 0,
      [ResponseCodeGroup.HTTP_4XX]: 78,
      [ResponseCodeGroup.HTTP_5XX]: 3,
      [ResponseCodeGroup.UNKNOWN]: 0,
    }
  }
];

storiesOf('Service stats', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, ServiceStatsModule, RouterTestingModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px; width: 800px">
<app-service-stats [operationStats]="operationStats"></app-service-stats>
</div>`,
    props: {
      operationStats
    }
  };
});
