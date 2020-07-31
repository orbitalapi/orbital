import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-attribute-panel',
  template: `
  <div class="container">
    <div class="header">
      <mat-form-field class="example-form-field">
        <mat-label>Clearable input</mat-label>
        <input matInput type="text">
      </mat-form-field>
      <div class="key-value-pill">
        <span class="key">Column</span>
        <span class="value">2</span>
      </div>
    </div>
    <div class="type-selector">
      <app-type-autocomplete></app-type-autocomplete>
    </div>
    <div class="samples">
      <h3>Sample values</h3>
    </div>
  </div>
  `,
  styleUrls: ['./attribute-panel.component.scss']
})
export class AttributePanelComponent implements OnInit {

  constructor() { }

  ngOnInit() {
  }

}
