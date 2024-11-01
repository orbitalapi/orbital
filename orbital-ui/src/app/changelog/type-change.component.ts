import { Component, Input, OnInit } from '@angular/core';
import { QualifiedName } from '../services/schema';

@Component({
  selector: 'app-type-change',
  template: `
    <h4 class="h4">{{ label }}</h4>
    <div class="container">
      <span class="old-value mono-badge">{{ oldValue.shortDisplayName }}</span>
      <tui-icon icon="@tui.arrow-right" class="icon"></tui-icon>
      <span class="new-value mono-badge">{{ newValue.shortDisplayName }}</span>
    </div>

  `,
  styleUrls: ['./type-change.component.scss']
})
export class TypeChangeComponent {

  @Input()
  label: string;

  @Input()
  oldValue: QualifiedName;

  @Input()
  newValue: QualifiedName;
}
