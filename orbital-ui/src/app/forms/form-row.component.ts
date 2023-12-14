import {Component, Input, ViewEncapsulation} from '@angular/core';

@Component({
  selector: 'app-form-row',
  template: `
    <div class="form-row">
      <div class="form-item-description-container">
        <h3>{{ title }}</h3>
        <div class="help-text">
          {{ helpText }}
        </div>
      </div>
      <ng-content></ng-content>
    </div>
  `,
  styleUrls: ['./form-row.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class FormRowComponent {

  @Input()
  title: string;

  @Input()
  helpText: string;

  @Input()
  useFormField = true
}
