import {ChangeDetectionStrategy, Component} from '@angular/core';
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {AngularSplitModule} from "angular-split";
import {PolicyCodeEditorPanelComponent} from "./policy-code-editor-panel/policy-code-editor-panel.component";
import {PolicyQueryEditorPanelComponent} from "./policy-query-editor-panel/policy-query-editor-panel.component";
import {CompilationMessageListModule} from "../../compilation-message-list/compilation-message-list.module";

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
            <app-policy-code-editor-panel></app-policy-code-editor-panel>
          </as-split-area>
          <as-split-area>
            <app-policy-query-editor-panel></app-policy-query-editor-panel>
          </as-split-area>
        </as-split>
      </as-split-area>
    </as-split>

  `,
  styleUrl: './policy-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PolicyEditorComponent {

}
