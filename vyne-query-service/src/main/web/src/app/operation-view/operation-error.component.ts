import {Component, Input, OnInit} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';

@Component({
  selector: 'app-operation-error',
  template: `
    <div class="error-message-box">
      <span>{{operationError.message}} - {{operationError?.error?.message}}</span>
    </div>
  `,
  styleUrls: ['./operation-error.component.scss']
})
export class OperationErrorComponent {
  @Input()
  operationError: HttpErrorResponse;
}
