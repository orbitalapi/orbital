import {Component, Inject, Input, LOCALE_ID} from '@angular/core';
import {RemoteOperationPerformanceStats, ResponseCodeCountMap, ResponseCodeGroup} from '../services/query.service';
import {isNullOrUndefined} from 'util';
import {formatNumber} from '@angular/common';

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
        <td>{{ statRow.averageTimeToFirstResponse | number: '1.0-0' }}ms</td>
        <td>{{ totalWaitTime(statRow)  }}</td>
        <td>
          <div class="pill" *ngFor="let responseCode of filterResponseCodes(statRow.responseCodes) | keyvalue"
               [ngClass]="responseCode.value.cssClass">
            <span class="key">{{ responseCode.key }}</span>
            <span class="value">{{responseCode.value.data}}</span>
          </div>
        </td>
      </tr>
      </tbody>
      <tfoot>
      <tr>
        <th>
          Summary
        </th>
        <th>{{ summary.callsInitiated }}</th>
        <th></th>
        <th>{{ summary.totalWaitTime  | number }}ms</th>
        <th>
        </th>
      </tr>
      </tfoot>
    </table>`,
  styleUrls: ['./service-stats.component.scss']
})
export class ServiceStatsComponent {

  summary: Partial<RemoteOperationPerformanceStats>;

  constructor(@Inject(LOCALE_ID) private locale: string) {
  }

  private _operationStats: RemoteOperationPerformanceStats[] = [];

  @Input()
  get operationStats(): RemoteOperationPerformanceStats[] {
    return this._operationStats;
  }

  set operationStats(value: RemoteOperationPerformanceStats[]) {
    if (this._operationStats === value) {
      return;
    }
    this._operationStats = value;
    this.calculateSummary();
  }

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
      return '' + formatNumber(statRow.totalWaitTime, this.locale) + 'ms';
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

  private calculateSummary() {
    let totalWaitTime = 0;
    let totalCalls = 0;
    this.operationStats.forEach(row => {
      totalWaitTime += (row.totalWaitTime || 0);
      totalCalls += row.callsInitiated;
    });

    this.summary = {
      callsInitiated: totalCalls,
      totalWaitTime: totalWaitTime
    };
  }
}
