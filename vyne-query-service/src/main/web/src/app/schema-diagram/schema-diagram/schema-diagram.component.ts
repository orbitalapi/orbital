import { ChangeDetectionStrategy, Component, ElementRef, Input, ViewChild } from '@angular/core';
import { findSchemaMember, findType, QualifiedName, Schema, tryFindType } from '../../services/schema';
import { REACT_FLOW_TEST_STATE, SchemaChartState, SchemaFlowWrapper } from './schema-flow.react';
import { findMember } from '@angular/compiler-cli/src/ngtsc/reflection/src/typescript';
import { buildSchemaChart } from './schema-chart-builder';

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
    if (!this.schema || !this.displayedMembers || !this.containerRef) {
      return;
    }

    const schemaChartState: SchemaChartState = buildSchemaChart(this.schema, this.displayedMembers)

    SchemaFlowWrapper.initialize(
      this._containerRef,
      schemaChartState
    )
  }

}

