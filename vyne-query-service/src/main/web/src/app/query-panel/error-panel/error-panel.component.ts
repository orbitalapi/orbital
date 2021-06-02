import {Component, Input} from '@angular/core';
import {FailedSearchResponse, QueryResult} from '../../services/query.service';
import {QueryFailure} from '../query-wizard/query-wizard.component';

@Component({
  selector: 'app-error-panel',
  template: `
    <div class="error-message-box">
      <img src="assets/img/no-path.svg">
      <div>
        <h3>An error occurred running your query:</h3>
        <div>{{ queryResult.message }}</div>
      </div>
    </div>
  `,
  styleUrls: ['./error-panel.component.scss']
})
export class ErrorPanelComponent {

  @Input()
  queryResult: FailedSearchResponse;
}
