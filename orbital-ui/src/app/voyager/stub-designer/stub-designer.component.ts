import {ChangeDetectionStrategy, Component, Inject, Input} from '@angular/core';
import {Operation, Schema} from "../../services/schema";
import {TuiSegmentedModule} from "@taiga-ui/experimental";
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {NgForOf, NgIf} from "@angular/common";
import {SimpleCodeEditorComponent} from "../../simple-code-editor/simple-code-editor.component";
import {TuiAccordionModule, TuiInputModule} from "@taiga-ui/kit";
import {TuiButtonModule, TuiDialogContext, TuiHintModule, TuiTextfieldControllerModule} from "@taiga-ui/core";
import {FormsModule} from "@angular/forms";
import {OperationStub, ParameterValue, ResponseCondition} from "../../services/query.service";
import {isNullOrUndefined} from "../../utils/utils";
import {POLYMORPHEUS_CONTEXT} from "@tinkoff/ng-polymorpheus";

@Component({
  selector: 'app-stub-designer',
  standalone: true,
  imports: [
    ExpandingPanelSetModule,
    TuiSegmentedModule,
    NgIf,
    SimpleCodeEditorComponent,
    NgForOf,
    TuiInputModule,
    TuiTextfieldControllerModule,
    TuiAccordionModule,
    TuiButtonModule,
    FormsModule,
    TuiHintModule
  ],
  template: `
    <app-panel-header tablerIcon="switch-horizontal" title="Stub response editor">
      <div class="spacer"></div>
      <tui-segmented size="s" [(activeItemIndex)]="viewModeActiveIndex" class="dark">
        <button [class.active]="viewModeActiveIndex === 0" tuiHint="Configure a response used for all requests">Simple
        </button>
        <button [class.active]="viewModeActiveIndex === 1" [disabled]="advancedModeDisabled"
                [tuiHint]="advancedModeHint" [class.disabled]="advancedModeDisabled">Advanced
        </button>
      </tui-segmented>
    </app-panel-header>
    <div class="panel-body">
      <div *ngIf="viewModeActiveIndex == 1" class="form-view">
        <div>Configure the responses returned based on the input parameters</div>
        <div class="row">
          <div class="form-section-label">Responses</div>
          <div class="spacer"></div>
          <tui-segmented size="s" [(activeItemIndex)]="advancedViewEditModeIndex">
            <button [class.active]="advancedViewEditModeIndex === 0">Form</button>
            <button [class.active]="advancedViewEditModeIndex === 1">JSON</button>
          </tui-segmented>
        </div>

        <div class="advanced-mode-form-container" *ngIf="advancedViewEditModeIndex===0">
          <tui-accordion>
            <tui-accordion-item *ngFor="let condition of conditionalResponses">
              <div class="parameter-summary-row">
                <div class="parameter-summary" *ngFor="let param of condition.inputs">
                  <span class="parameter-name">{{ param.name }}:</span>
                  <span class="parameter-value">{{ param.value }}</span>
                </div>
                <div class="spacer"></div>
                <button tuiButton appearance="icon" icon="tuiIconTrash" size="xs"
                        (click)="removeCondition(condition)"></button>
              </div>
              <ng-template tuiAccordionItemContent>
                <div class="response-condition-editor">
                  <div class="form-section-label">Parameters</div>
                  <div class="parameters">
                    <div class="param-row row" *ngFor="let input of condition.inputs">
                      <div class="label">{{ input.name }} <span
                        class="type-name type">{{ paramType(input) }}</span></div>
                      <tui-input tuiTextfieldSize="m" [tuiTextfieldLabelOutside]="true"><input tuiTextfield
                                                                                               [(ngModel)]="input.value"/>
                      </tui-input>
                    </div>
                  </div>
                  <div class="form-section-label">Response body</div>
                  <app-simple-code-editor [(content)]="condition.response.body"></app-simple-code-editor>
                </div>

              </ng-template>
            </tui-accordion-item>
          </tui-accordion>
          <div class="row items-center">
            <button tuiButton appearance="outline" size="s" (click)="addNewCondition()">Add new condition</button>
          </div>

        </div>
        <div class="advanced-mode-json-editor" *ngIf="advancedViewEditModeIndex===1">
          <app-simple-code-editor [(content)]="advancedModeJson"></app-simple-code-editor>
        </div>
      </div>
      <div *ngIf="viewModeActiveIndex == 0" class="simple-view">
        <div>Paste a response which will be used for every call to {{ operation?.name }}</div>
        <app-simple-code-editor [(content)]="operationStub.response"></app-simple-code-editor>
      </div>
      <div class="row">
        <button tuiButton size="m" appearance="outline" (click)="context.completeWith(context.data.stub)">Cancel
        </button>
        <span class="spacer"></span>
        <button tuiButton size="m" appearance="primary" (click)="context.completeWith(operationStub)">Update</button>
      </div>
    </div>
  `,
  styleUrl: './stub-designer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StubDesignerComponent {

  constructor(@Inject(POLYMORPHEUS_CONTEXT)
              readonly context: TuiDialogContext<OperationStub, StubDesignerProps>,) {
    this.operationStub = JSON.parse(JSON.stringify(context.data.stub));
    this.operation = context.data.operation;
  }

  get advancedModeJson() {
    return JSON.stringify(this.operationStub.conditionalResponses, null, 2);
  }
  set advancedModeJson(value) {
    try {
      this.operationStub.conditionalResponses = JSON.parse(value);
    } catch (e) {
      console.error('Failed to update conditional responses', e)
    }

  }

  get advancedModeDisabled() {
    return isNullOrUndefined(this.operation?.parameters) || this.operation.parameters.length == 0;
  }
  get advancedModeHint() {
    if (this.advancedModeDisabled) {
      return `Disabled as ${this.operation.qualifiedName.shortDisplayName} has no parameters`
    } else {
      return 'Configure different responses based on inputs'
    }
  }

  advancedViewEditModeIndex = 0;
  viewModeActiveIndex = 0;
  operation: Operation;
  operationStub: OperationStub;

  get conditionalResponses():ResponseCondition[] {
    return this.operationStub?.conditionalResponses || [];
  }


  get parameters() {
    return this.operation?.parameters || [];
  }

  addNewCondition() {
    if (isNullOrUndefined(this.operationStub.conditionalResponses)) {
      this.operationStub.conditionalResponses = [];
    }
    this.operationStub.conditionalResponses.push({
      inputs: this.parameters.map((param) => {
        return {
          name: param.name,
          value: null
        }
      }),
      response: {
        body: ''
      }
    })
  }

  removeCondition(condition: ResponseCondition) {
    this.operationStub.conditionalResponses.splice(this.operationStub.conditionalResponses.indexOf(condition), 1)
  }

  paramType(input: ParameterValue): string {
    return this.operation.parameters.find((param) => param.name === input.name)
      ?.typeName?.shortDisplayName
  }
}

export interface StubDesignerProps {
  operation: Operation;
  stub: OperationStub;
}

