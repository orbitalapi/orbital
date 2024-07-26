import {Component} from '@angular/core';
import {CompilationMessage, Schema, SchemaMember, Type} from '../services/schema';
import {TypesService} from "../services/types.service";
import {Observable} from "rxjs/internal/Observable";

@Component({
  selector: 'app-model-designer',
  template: `
    <app-panel-header title="Model designer" helpText="- Create and edit Taxi schemas for your data that you can copy into your project"></app-panel-header>
    <as-split direction="vertical" unit="percent">
      <as-split-area>
        <as-split direction="horizontal" unit="percent">
          <as-split-area>
            <app-designer-source-input-panel [(content)]="sourceContent" (contentCleared)="targetType = null"></app-designer-source-input-panel>
          </as-split-area>
          <as-split-area>
            <app-designer-code-editor-panel [schema]="schema$ | async"
                                            [parsedTypes]="parsedTypes"
                                            (selectedTypeChanged)="this.targetType = $event"
                                            [compilationErrors]="compilationErrors"
                                            (taxiChange)="taxi = $event"
                                            [disabled]="!sourceContent"
            ></app-designer-code-editor-panel>
          </as-split-area>
        </as-split>
      </as-split-area>
      <as-split-area>
        <app-designer-parse-result-panel
            [schema]="schema$ | async"
            [source]="sourceContent"
            [taxi]="taxi"
            [targetType]="targetType?.name.fullyQualifiedName"
            (parsedTypesChanged)="this.parsedTypes = $event"
            (compilationErrorsChanged)="this.compilationErrors = $event"
            [disabled]="!targetType"
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
  targetType: SchemaMember;

  parsedTypes: Type[] = [];
  compilationErrors: CompilationMessage[] = [];
}
