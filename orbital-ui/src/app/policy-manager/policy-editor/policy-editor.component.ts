import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {AngularSplitModule} from "angular-split";
import {PolicyCodeEditorPanelComponent} from "./policy-code-editor-panel/policy-code-editor-panel.component";
import {PolicyQueryEditorPanelComponent} from "./policy-query-editor-panel/policy-query-editor-panel.component";
import {CompilationMessageListModule} from "../../compilation-message-list/compilation-message-list.module";
import {CompilationMessage} from "../../services/schema";

@Component({
  selector: 'app-policy-editor',
  standalone: true,
  imports: [
    ExpandingPanelSetModule,
    AngularSplitModule,
    PolicyCodeEditorPanelComponent,
    PolicyQueryEditorPanelComponent,
    CompilationMessageListModule
  ],
  template: `
    <app-panel-header title="Policy designer" helpText="- Create and test policies that control how data is accessed"></app-panel-header>
    <as-split direction="vertical" unit="pixel">
        <as-split-area>
          <as-split direction="horizontal">
            <as-split-area>
              <app-policy-code-editor-panel (compilationMessagesUpdated)="policyEditorCompilationMessagesUpdated($event)"></app-policy-code-editor-panel>
            </as-split-area>
            <as-split-area>
              <app-policy-query-editor-panel (compilationMessagesUpdated)="queryEditorCompilationMessagesUpdated($event)"></app-policy-query-editor-panel>
            </as-split-area>
          </as-split>
        </as-split-area>
      <as-split-area size="150">
        <app-compilation-message-list [compilationMessages]="compilationMessages"></app-compilation-message-list>
      </as-split-area>
    </as-split>

  `,
  styleUrl: './policy-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PolicyEditorComponent {

  constructor(private changeDetector: ChangeDetectorRef) {
  }
  private queryEditorCompilationMessages: CompilationMessage[] = [];
  private policyEditorCompilationMessages: CompilationMessage[] = [];


  /**
   * Returns the combined compilation messages from both editors.
   *
   * Keeping two seperate arrays and then combining them might seem convoluted,
   * but we do this because each editor "owns" it's own compilation messages, and
   * emits the complete state each time, so we need to always combine both result sets
   */
  get compilationMessages():CompilationMessage[] {
    return [
      ...this.queryEditorCompilationMessages,
      ...this.policyEditorCompilationMessages
    ]
  }
  queryEditorCompilationMessagesUpdated($event: CompilationMessage[]) {
    this.queryEditorCompilationMessages = $event;
    this.changeDetector.markForCheck();
  }

  policyEditorCompilationMessagesUpdated($event: CompilationMessage[]) {
   this.policyEditorCompilationMessages = $event;
    this.changeDetector.markForCheck();
  }
}
