import { ChangeDetectionStrategy, Component, ElementRef, Input, ViewChild } from '@angular/core';
import { findSchemaMember, findType, Schema } from '../../services/schema';
import { SchemaChartState, SchemaFlowWrapper } from './schema-flow.react';
import { buildSchemaChart } from './schema-chart-builder';
import { SchemaChartController } from './schema-chart.controller';

@Component({
  selector: 'app-schema-diagram',
  styleUrls: ['./schema-diagram.component.scss'],
  template: `
    <div #container class="container"></div>
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
    // const controller = new SchemaChartController(this.schema);
    // this.displayedMembers.forEach(member => {
    //   controller.ensureMemberPresent(findSchemaMember(this.schema, member))
    // })

    SchemaFlowWrapper.initialize(
      this._containerRef,
      this.displayedMembers,
      this.schema
    )
  }

}

