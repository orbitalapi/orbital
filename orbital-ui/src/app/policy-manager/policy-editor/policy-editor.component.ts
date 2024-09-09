import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, Component, Inject, Injector} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {TuiDialogService, TuiLinkModule} from '@taiga-ui/core';
import {filter, Observable, switchMap} from 'rxjs';
import {map} from 'rxjs/operators';
import {UiCustomisations} from '../../../environments/ui-customisations';
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {AngularSplitModule} from "angular-split";
import {Policy, TypesService} from '../../services/types.service';
import {PolicyCodeEditorPanelComponent} from "./policy-code-editor-panel/policy-code-editor-panel.component";
import {PolicyQueryEditorPanelComponent} from "./policy-query-editor-panel/policy-query-editor-panel.component";
import {CompilationMessageListModule} from "../../compilation-message-list/compilation-message-list.module";

@Component({
  selector: 'app-policy-editor',
  standalone: true,
  imports: [
    CommonModule,
    ExpandingPanelSetModule,
    AngularSplitModule,
    PolicyCodeEditorPanelComponent,
    PolicyQueryEditorPanelComponent,
    CompilationMessageListModule,
    TuiLinkModule
  ],
  template: `
    <app-panel-header title="Policy designer" helpText="- Create and test policies that control how data is accessed">
      <a
        icon="tuiIconHelpCircle"
        tuiLink
        class="help-link"
        [href]="UiCustomisations.docsLinks.dataPolicies"
        target="_blank"
      >
        Help
      </a>
    </app-panel-header>
    <as-split direction="vertical" unit="pixel">
      <as-split-area>
        <as-split direction="horizontal">
          <as-split-area>
            <app-policy-code-editor-panel
              [versionedSource]="(policy$ | async)?.sourceFile"
              (policyNeedsSaving)="isPolicyDirty = $event"
            ></app-policy-code-editor-panel>
          </as-split-area>
          <as-split-area>
            <app-policy-query-editor-panel [policyNeedsSaving]="isPolicyDirty"></app-policy-query-editor-panel>
          </as-split-area>
        </as-split>
      </as-split-area>
    </as-split>

  `,
  styleUrl: './policy-editor.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PolicyEditorComponent {
  policy$: Observable<Policy>
  policyName$: Observable<string>
  isPolicyDirty: boolean;

  constructor(
    private activatedRoute: ActivatedRoute,
    private typeService: TypesService,
    @Inject(TuiDialogService) private readonly dialogs: TuiDialogService,
    @Inject(Injector) private readonly injector: Injector,
  ) {
    this.policyName$ = activatedRoute.paramMap.pipe(
      map(paramMap => {
        return paramMap.get('policyName');
      }),
      filter(policyName => !!policyName)
    )
    this.policy$ = this.policyName$.pipe(
      switchMap(endpoint => typeService.getPolicy(endpoint))
    )
  }

  protected readonly UiCustomisations = UiCustomisations;
}
