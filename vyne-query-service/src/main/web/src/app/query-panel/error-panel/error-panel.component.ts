import {Component, Input} from '@angular/core';
import {QueryResult} from '../../services/query.service';

@Component({
  selector: 'app-error-panel',
  template: `
    <div class="error-message-box" *ngIf="queryResult?.unmatchedNodes?.length > 0">
      <img src="assets/img/no-path.svg">
      <div>
        <span>Vyne wasn't able to find a way to load the following type(s):</span>
        <ul>
          <li *ngFor="let name of queryResult?.unmatchedNodes">{{name.longDisplayName}}</li>
        </ul>
        <span>This can happen:</span>
        <ul>
          <li>
            if there's no service running which returns this data. If you specified criteria for this type,
            then a service must also accept those inputs as parameters in order to be called. Goto Schema Explorer and
            check relevant Cask service schemas.
          </li>
          <li>If the filter criteria values (e.g. date ranges) that you pass into your query are invalid. Make sure that
            relevant cask(s) have data for given filters.
          </li>
        </ul>
      </div>
    </div>
  `,
  styleUrls: ['./error-panel.component.scss']
})
export class ErrorPanelComponent {

  @Input()
  queryResult: QueryResult | null;
}
