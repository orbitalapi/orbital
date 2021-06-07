import {Component, Input} from '@angular/core';
import {RemoteOperationPerformanceStats, ResponseCodeCountMap, ResponseCodeGroup} from '../services/query.service';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-service-stats',
  template: `
    <table class="service-stats-table">
      <thead>
      <tr>
        <th>Operation</th>
        <th># Calls</th>
        <th>Avg. time to respond</th>
        <th>Total wait time</th>
        <th>Response codes</th>
      </tr>
      </thead>
      <tbody>
      <tr *ngFor="let statRow of operationStats">
        <td>
          <div class="badges">
              <span class="mono-badge">
              <a [routerLink]="['/services',statRow.serviceName]">{{statRow.serviceName}}</a>
              </span>
            <span class="separator-slash">/</span>
            <span class="mono-badge">
<a
  [routerLink]="['/services',statRow.serviceName, statRow.operationName]">{{statRow.operationName}}</a>
</span>
          </div>
        </td>
        <td>{{ statRow.callsInitiated }}</td>
        <td>{{ statRow.averageTimeToFirstResponse }}ms</td>
        <td>{{ totalWaitTime(statRow) }}</td>
        <td>
          <div class="pill" *ngFor="let responseCode of filterResponseCodes(statRow.responseCodes) | keyvalue"
               [ngClass]="responseCode.value.cssClass">
            <span class="key">{{ responseCode.key }}</span>
            <span class="value">{{responseCode.value.data}}</span>
          </div>
        </td>
      </tr>
      </tbody>
    </table>`,
  styleUrls: ['./service-stats.component.scss']
})
export class ServiceStatsComponent {

  @Input()
  operationStats: RemoteOperationPerformanceStats[];

  private pillClasses = {
    [ResponseCodeGroup.HTTP_2XX]: 'code_2xx',
    [ResponseCodeGroup.HTTP_3XX]: 'code_3xx',
    [ResponseCodeGroup.HTTP_4XX]: 'code_4xx',
    [ResponseCodeGroup.HTTP_5XX]: 'code_5xx'
  };

  totalWaitTime(statRow: RemoteOperationPerformanceStats) {
    if (isNullOrUndefined(statRow.totalWaitTime)) {
      return 'Streaming response';
    } else {
      return '' + statRow.totalWaitTime + 'ms';
    }
  }

  filterResponseCodes(responseCodeMap: ResponseCodeCountMap): any {
    const result = {};
    Object.keys(responseCodeMap).forEach(responseCode => {
      if (responseCodeMap[responseCode] > 0) {
        result[this.responseCodeLabel(responseCode)] = {
          data: responseCodeMap[responseCode],
          cssClass: this.pillClasses[responseCode]
        };
      }
    });
    return result;

  }


  responseCodeLabel(key: ResponseCodeGroup | string) {
    switch (key) {
      case ResponseCodeGroup.HTTP_2XX:
        return `2xx`;
      case ResponseCodeGroup.HTTP_3XX:
        return `3xx`;
      case ResponseCodeGroup.HTTP_4XX:
        return `4xx`;
      case ResponseCodeGroup.HTTP_5XX:
        return `5xx`;
    }
  }
}
