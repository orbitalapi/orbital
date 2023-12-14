import {Component, Input} from '@angular/core';
import {FailedSearchResponse} from '../../services/models';

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
