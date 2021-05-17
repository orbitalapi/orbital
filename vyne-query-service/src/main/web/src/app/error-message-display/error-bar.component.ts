import {Component, OnInit, Input} from '@angular/core';

@Component({
  selector: 'app-error-message',
  template: `
    <div class="error-message">
      <mat-icon>error_outline</mat-icon>
      {{message}}</div>
  `,
  styleUrls: ['./error-bar.component.scss']
})
export class ErrorBarComponent {

  @Input()
  message: string;

}
