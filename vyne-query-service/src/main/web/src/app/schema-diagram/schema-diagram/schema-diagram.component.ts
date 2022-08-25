import { ChangeDetectionStrategy, Component, ElementRef, Input, ViewChild } from '@angular/core';
import { QualifiedName, Schema } from '../../services/schema';
import { REACT_FLOW_TEST_STATE, SchemaFlowWrapper } from './schema-flow.react';

@Component({
  selector: 'app-schema-diagram',
  styleUrls: ['./schema-diagram.component.scss'],
  template: `
    <div #container class="container">Hello, World</div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SchemaDiagramComponent {
  private _displayedMembers: string[];
  @Input()
  get displayedMembers(): string[] {
    return this._displayedMembers;
  }

  set displayedMembers(value: string[]) {
    this._displayedMembers = value;
    this.resetComponent();
  }

  @Input()
  schema: Schema;


  private _containerRef: ElementRef;

  @ViewChild('container')
  get containerRef(): ElementRef {
    return this._containerRef;
  }

  set containerRef(value: ElementRef) {
    if (this._containerRef === value) {
      return;
    }
    this._containerRef = value;
    this.resetComponent();
  }

  resetComponent() {
    if (!this.schema || !this.displayedMembers) {
      return;
    }

    SchemaFlowWrapper.initialize(
      this._containerRef,
      REACT_FLOW_TEST_STATE
    )
  }
}
