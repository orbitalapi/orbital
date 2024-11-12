import {Component, Input} from '@angular/core';
import {CodeEditorModule} from "../../../code-editor/code-editor.module";
import {ExpandingPanelSetModule} from "../../../expanding-panelset/expanding-panel-set.module";
import {QueryPanelModule} from "../../../query-panel/query-panel.module";
import {AngularSplitModule} from "angular-split";
import {TabbedResultsViewModule} from "../../../tabbed-results-view/tabbed-results-view.module";
import {QueryEditorStoreService} from "../../../services/query-editor-store.service";
import {QueryEditorPayload, QueryEditorState} from "../../../services/query-editor.state";
import {TypesService} from "../../../services/types.service";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {Schema} from "../../../services/schema";
import {NgIf} from "@angular/common";
import { TuiNotification, TuiButton, TuiHint } from '@taiga-ui/core';

@Component({
  selector: 'app-policy-query-editor-panel',
  standalone: true,
  imports: [
    CodeEditorModule,
    ExpandingPanelSetModule,
    QueryPanelModule,
    AngularSplitModule,
    TabbedResultsViewModule,
    NgIf,
    TuiButton,
    TuiNotification,
    TuiHint
  ],
  template: `
    <app-panel-header title="Query editor" [isSecondary]="true">
      <div class="spacer"></div>
      <div [tuiHint]="policyNeedsSaving ? 'Save the policy before running the query' : null" tuiHintAppearance="dark">
        <button tuiButton size="s" appearance="primary"
                class='button-small menu-bar-button'
                [class.is-disabled]="!queryState.query() || policyNeedsSaving"
                *ngIf="queryState.currentState() !== 'Running' && queryState.currentState() !== 'Cancelling'"
                (click)='runQuery()'

        >
          <img src="assets/img/tabler/player-play.svg" class='filter-white'>
          Run
        </button>
      </div>
      <div *ngIf="queryState.currentState() === 'Running'">
        <button tuiButton size="s" appearance="outline-grayscale"
                class='button-small menu-bar-button'
                (click)='cancelQuery()'
        >
          <img src="assets/img/tabler/player-stop.svg">
          Cancel
        </button>
      </div>
      <div *ngIf="queryState.currentState() === 'Cancelling'">
        <button tuiButton size="s" appearance="outline-grayscale"
                class='button-small menu-bar-button'
                [disabled]='true'
        >
        <span class='running-timer'>
          <span class='loader'></span>
          <span>Cancelling...</span>
        </span>
        </button>
      </div>
    </app-panel-header>
    <tui-notification size="m" appearance="info" *ngIf="showQueryEditorHelp">
      @if (policyNeedsSaving) {
        You have unsaved changes. Save your policy for it to be applied to query results
      } @else {
        Test that the policy is giving the desired output by running a query here.
      }
      <button (click)="showQueryEditorHelp = false" tuiIconButton iconStart="@tui.x"></button>
    </tui-notification>
    <as-split direction="vertical">
      <as-split-area>
        <app-code-editor
          [content]="queryState.query()"
          [showCompilationProblemsPanel]="true"
          [setFocus]="false"
          (contentChange)="updateQuery($event)"
        ></app-code-editor>
      </as-split-area>
      <as-split-area>
        <app-tabbed-results-view [instances$]='queryState.potentiallyPausedResults()'
                                 [profileData$]='queryState.queryProfileData()'
                                 [queryPlanData$]='queryState.queryPlanData()'
                                 [errorMessages$]="queryState.errors()"
                                 [errorCount]="queryState.errorCount()"
                                 [type]="queryState.resultType()"
                                 [anonymousTypes]="queryState.anonymousTypes()"
                                 [isQueryRunning]='queryState.currentState() === "Running"'
                                 [isStreamingQuery]="false"
                                 [isQueryPaused]="queryState.isQueryPaused()"
                                 [showMaxRecordCountWarning]="queryState.showMaxRecordCountWarning()"
                                 [queryStartTime]="queryState.queryStartTime()"
                                 (instanceSelected)='queryState.valuePanelVisible.set(true); queryState.instanceSelected().next($event)'
                                 [downloadSupported]="false"
        ></app-tabbed-results-view>
      </as-split-area>
    </as-split>


  `,
  styleUrl: './policy-query-editor-panel.component.scss'
})
export class PolicyQueryEditorPanelComponent {
  @Input()
  policyNeedsSaving: boolean;

  queryEditorState: QueryEditorState;
  schema: Schema
  showQueryEditorHelp: boolean = true;

  get queryState():QueryEditorPayload {
    return this.queryEditorState?.payload
  }

  constructor(
    private queryEditorStore: QueryEditorStoreService,
    private typeService: TypesService,
  ) {
    this.queryEditorState = queryEditorStore.createTemporaryQuery()
    typeService.getTypes()
      .pipe(takeUntilDestroyed())
      .subscribe(schema => {
        this.schema = schema;
      });
  }

  runQuery() {
    this.queryEditorState.submitQuery("TaxiQL", this.schema);
  }

  cancelQuery() {
    this.queryEditorState.cancelQuery()
  }

  updateQuery($event: string) {
    this.queryState.query.set($event);
  }
}
