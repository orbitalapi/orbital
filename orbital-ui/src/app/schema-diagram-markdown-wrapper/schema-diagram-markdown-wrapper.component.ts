import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input} from '@angular/core';
import {SchemaDiagramModule} from "../schema-diagram/schema-diagram.module";
import {SCHEMA_PROVIDER_TOKEN, SchemaProvider, TypesService} from "../services/types.service";
import {Observable} from "rxjs";
import {Schema} from "../services/schema";
import {isNullOrUndefined} from "../utils/utils";


/**
 * Component used within markdown display, to enable rendering of
 * Orbital schema diagrams.
 *
 * This component has a few responsibilities:
 *  - Parsing the markdown JSON into parameters to render the diagram
 *  - Injecting CSS to undo some leaky / global styling injected by our markdown renderer - marked - which affect
 *    the display of the charts
 */
@Component({
  selector: 'app-schema-diagram-markdown-wrapper',
  standalone: true,
  imports: [
    SchemaDiagramModule
  ],
  template: `
    <app-schema-diagram
      [schema$]="schema$"
      [memberNameNavigable]="true"
      [displayedMembers]="displayedMembers"
      [showTypeToolbar]="diagramSpec?.showTypeToolbar"
      hasBorder="true"
    ></app-schema-diagram>
  `,
  styleUrl: './schema-diagram-markdown-wrapper.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchemaDiagramMarkdownWrapperComponent {
  schema$: Observable<Schema>;

  constructor(@Inject(SCHEMA_PROVIDER_TOKEN) private schemaProvider: SchemaProvider, changeDetector: ChangeDetectorRef) {
    this.schema$ = schemaProvider.getSchema();
  }

  private _code: string;
  diagramSpec: SchemaDiagramSpec;

  @Input()
  get code(): string {
    return this._code;
  }

  set code(value: string) {
    this._code = value;
    this.parseCode();
  }



  private parseCode() {
    try {
      const parsedDiagramSpec = JSON.parse(this.code) as SchemaDiagramSpec;
      const defaultDiagramSpec: SchemaDiagramSpec = {
        members: {},
        showTypeToolbar: true
      }
      this.diagramSpec = {
        ...defaultDiagramSpec,
        ...parsedDiagramSpec
      }
      this.displayedMembers = this.parseDisplayedMembers();
    } catch (e) {
      console.log(e, 'Diagram is invalid')
    }
  }

  displayedMembers: string[] | 'everything' | 'services' = 'everything'

  parseDisplayedMembers():string[] {
    if (isNullOrUndefined(this.diagramSpec)) {
      return [];
    }
    try {
      return Object.keys(this.diagramSpec.members)
    } catch (e) {
      console.log(e, 'Diagram is invalid')
      return [];
    }

  }
}

/**
 * The serialized form of a schema diagram.
 */
export interface SchemaDiagramSpec {
  /**
   * A map of members (using their qualified name), to a series
   * of display properties
   */
  members: { [key: string]: SchemaDiagramMemberDisplayProperties }
  showTypeToolbar: boolean
}

export interface SchemaDiagramMemberDisplayProperties {
  x?: number;
  y?: number;
}
