import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-key-value',
  template: `
    <div class="key-value-card">
      <div class="title">{{title}}</div>
      <div class="value">{{value}}</div>
    </div>
  `,
  styleUrls: ['./key-value.component.scss']
})
export class KeyValueComponent {

  @Input()
  title: string;

  @Input()
  value: string;
}
