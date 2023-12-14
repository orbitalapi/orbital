import {Component, OnInit} from '@angular/core';
import {CompilationMessage, Schema, Type} from "../services/schema";
import {TypesService} from "../services/types.service";
import {Observable} from "rxjs/internal/Observable";

@Component({
  selector: 'app-model-designer',
  template: `
    <as-split direction="vertical" unit="percent">
      <as-split-area>
        <as-split direction="horizontal" unit="percent">
          <as-split-area>
            <app-designer-source-input-panel [(content)]="sourceContent"></app-designer-source-input-panel>
          </as-split-area>
          <as-split-area>
            <app-designer-code-editor-panel [schema]="schema$ | async"
                                            [parsedTypes]="parsedTypes"
                                            (selectedTypeChanged)="this.targetType = $event"
                                            [compilationErrors]="compilationErrors"
                                            (taxiChange)="taxi = $event"></app-designer-code-editor-panel>
          </as-split-area>
        </as-split>
      </as-split-area>
      <as-split-area>
        <app-designer-parse-result-panel
            [schema]="schema$ | async"
            [source]="sourceContent"
            [taxi]="taxi"
            [targetType]="targetType"
            (parsedTypesChanged)="this.parsedTypes = $event"
            (compilationErrorsChanged)="this.compilationErrors = $event"
        ></app-designer-parse-result-panel>
      </as-split-area>
    </as-split>
  `,
  styleUrls: ['./model-designer.component.scss']
})
export class ModelDesignerComponent {

  constructor(typeService: TypesService) {
    this.schema$ = typeService.getTypes()
  }

  schema$: Observable<Schema>

  sourceContent: string;
  taxi: string;
  targetType: string;

  parsedTypes: Type[] = [];
  compilationErrors: CompilationMessage[] = [];
}
