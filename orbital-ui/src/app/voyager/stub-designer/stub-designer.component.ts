import {TuiTextfieldControllerModule, TuiInputModule} from "@taiga-ui/legacy";
import {ChangeDetectionStrategy, Component, Inject} from '@angular/core';
import {AngularSplitModule} from 'angular-split';
import {CodeEditorModule} from '../../code-editor/code-editor.module';
import {findMemberTypeOrType, findType, Operation, Schema, Type} from '../../services/schema';
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {CommonModule} from '@angular/common';
import {TypesService} from '../../services/types.service';
import {SimpleCodeEditorComponent} from "../../simple-code-editor/simple-code-editor.component";
import {TuiAccordion, TuiChip, TuiSegmented, TuiSwitch} from "@taiga-ui/kit";
import {TuiDialogContext, TuiNotification, TuiButton, TuiHint} from '@taiga-ui/core';
import {FormsModule} from "@angular/forms";
import {OperationStub, ParameterValue, ResponseCondition} from "../../services/query.service";
import {TypeViewerModule} from '../../type-viewer/type-viewer.module';
import {isNullOrUndefined} from "../../utils/utils";
import {POLYMORPHEUS_CONTEXT} from "@taiga-ui/polymorpheus";

@Component({
  selector: 'app-stub-designer',
  standalone: true,
  imports: [
    CommonModule,
    ExpandingPanelSetModule,
    TuiSegmented,
    SimpleCodeEditorComponent,
    TuiInputModule,
    TuiTextfieldControllerModule,
    TuiAccordion,
    TuiButton,
    FormsModule,
    TuiHint,
    AngularSplitModule,
    CodeEditorModule,
    TuiChip,
    TypeViewerModule,
    TuiNotification,
    TuiSwitch,
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
      <ng-container *ngIf="viewModeActiveIndex == 0">
        <tui-notification size="m" *ngIf="operationStub.conditionalResponses?.length > 0" appearance="info">
          You already have conditional stubs, they will be favoured over anything you configure in here
        </tui-notification>
        <div class="row">
          <div>Paste a response which will be used for every call to {{ operation?.name }}</div>
          <div class="spacer"></div>
          <div class="toggle-container" [tuiHint]="echoInputHint">
            <span>Echo input as response</span>
            <input
              [disabled]="!canUseEchoInput"
              tuiSwitch
              type="checkbox"
              [(ngModel)]="operationStub.echoInput"
              [showIcons]="true"
              size="s"
            />
          </div>
        </div>
        <as-split direction="horizontal" unit="percent" gutterSize="1">
          <div class="thin-splitter" *asSplitGutter="let isDragged = isDragged" [class.dragged]="isDragged">
            <div class="thin-splitter-gutter-icon"></div>
          </div>
          <as-split-area class="type-viewer">
            <app-type-viewer
              [type]="type"
              [schema]="schema"
              [showDocumentation]="false"
              [schemaMemberNavigable]="false"
              [showTags]="false"
              [showTaxi]="false"
              [showUsages]="false"
              [showInheritanceGraph]="false"
            ></app-type-viewer>
          </as-split-area>
          <as-split-area size="60" class="simple-view">
            <app-simple-code-editor [(content)]="operationStub.response"
                                    *ngIf="!operationStub.echoInput"></app-simple-code-editor>
            <div *ngIf="operationStub.echoInput" class="will-echo-panel">
              Operation will echo the provided input
            </div>
          </as-split-area>
        </as-split>
      </ng-container>
      <div *ngIf="viewModeActiveIndex == 1" class="form-view">
        <div>Configure the responses returned based on the input parameters</div>
        <div class="row">
          <h3>Responses:</h3>
          <div class="spacer"></div>
          <button
            tuiButton
            size="s"
            appearance="secondary"
            iconStart="@tui.plus"
            class="button-small"
            (click)="addNewCondition()"
          >
            Add new condition
          </button>
          <tui-segmented size="s" [(activeItemIndex)]="advancedViewEditModeIndex">
            <button [class.active]="advancedViewEditModeIndex === 0" [disabled]="operationStub?.echoInput">Form</button>
            <button [class.active]="advancedViewEditModeIndex === 1" [disabled]="operationStub?.echoInput">JSON</button>
          </tui-segmented>
        </div>
        <div class="advanced-mode-form-container" *ngIf="advancedViewEditModeIndex===0">
          <i *ngIf="conditionalResponses?.length === 0">No conditional responses, add one to get started</i>
          <tui-accordion>
            <tui-accordion-item *ngFor="let condition of conditionalResponses">
              <div class="parameter-summary-row">
                <div class="parameter-summary" *ngFor="let param of condition.inputs">
                  <span class="parameter-name">{{ param.name }}:</span>
                  <span class="parameter-value">{{ param.value }}</span>
                </div>
                <div class="spacer"></div>
                <button tuiButton appearance="icon" iconStart="@tui.trash" size="xs"
                        (click)="removeCondition(condition)"></button>
              </div>
              <ng-template tuiAccordionItemContent>
                <div class="response-condition-editor">
                  <div class="form-section-label">Parameters</div>
                  <div class="parameters">
                    <div class="row" *ngFor="let input of condition.inputs">
                      <div class="label">{{ input.name }}
                        <span class="type-name type">{{ paramType(input) }}</span>
                      </div>
                      <tui-input
                        tuiTextfieldSize="s"
                        [tuiTextfieldLabelOutside]="true"
                        class="parameter-input"
                      >
                        <input tuiTextfieldLegacy [(ngModel)]="input.value"/>
                      </tui-input>
                    </div>
                  </div>
                  <div class="form-section-label">Response body</div>
                  <app-simple-code-editor [(content)]="condition.response.body"></app-simple-code-editor>
                </div>
              </ng-template>
            </tui-accordion-item>
          </tui-accordion>
        </div>
        <div class="advanced-mode-json-editor" *ngIf="advancedViewEditModeIndex===1">
          <app-simple-code-editor [(content)]="advancedModeJson"></app-simple-code-editor>
        </div>
      </div>
      <div class="footer">
        <button tuiButton size="m" appearance="outline-grayscale" (click)="context.completeWith(context.data.stub)">
          Cancel
        </button>
        <button tuiButton size="m" appearance="primary" (click)="context.completeWith(operationStub)">Update</button>
      </div>
    </div>
  `,
  styleUrl: './stub-designer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StubDesignerComponent {
  advancedViewEditModeIndex = 0;
  viewModeActiveIndex = 0;
  operation: Operation;
  operationStub: OperationStub;
  schema: Schema;
  type: Type

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    readonly context: TuiDialogContext<OperationStub, StubDesignerProps>,
    readonly typeService: TypesService
  ) {
    this.operationStub = JSON.parse(JSON.stringify(context.data.stub));
    this.operation = context.data.operation;
    this.schema = context.data.schema;

    this.type = findMemberTypeOrType(this.schema, this.operation.returnTypeName)
  }

  get canUseEchoInput(): boolean {
    return this.operation.parameters.length == 1 &&
      this.operation.parameters[0].typeName.parameterizedName == this.operation.returnTypeName.parameterizedName
  }
  get echoInputHint(): string {
    if (!this.canUseEchoInput) {
      return `Requires an operation that takes a single parameter, with a return value of the same type as the input parameter`
    } else {
      return `When enabled, the stub simply echoes back the input provided`
    }
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

  get conditionalResponses(): ResponseCondition[] {
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
  schema: Schema;
}

