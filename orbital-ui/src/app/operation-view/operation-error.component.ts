import {Component, Input} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';

@Component({
  selector: 'app-operation-error',
  template: `
    <div class="error-message-box">
      <span>{{operationError.message}}</span>
    </div>
  `,
  styleUrls: ['./operation-error.component.scss'],
  standalone: true
})
export class OperationErrorComponent {
  @Input()
  operationError: HttpErrorResponse;
}
