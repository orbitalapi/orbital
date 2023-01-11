import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-documentation-change',
  template: `
    <table class="table">
      <tbody>
      <tr>
        <td>Old value</td>
        <td>{{oldValue}}</td>
      </tr>
      <tr>
        <td>New value</td>
        <td>{{ newValue}}</td>
      </tr>
      </tbody>
    </table>
  `,
  styleUrls: ['./documentation-change.component.scss']
})
export class DocumentationChangeComponent {

  @Input()
  oldValue: string;

  @Input()
  newValue: string;
}
