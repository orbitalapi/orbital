import {Component, EventEmitter, Output} from '@angular/core';
import {CodeEditorModule} from "../../../code-editor/code-editor.module";
import {ExpandingPanelSetModule} from "../../../expanding-panelset/expanding-panel-set.module";
import {QueryPanelModule} from "../../../query-panel/query-panel.module";
import {AngularSplitModule} from "angular-split";
import {TabbedResultsViewModule} from "../../../tabbed-results-view/tabbed-results-view.module";
import {QueryEditorStoreService} from "../../../services/query-editor-store.service";
import {QueryEditorPayload, QueryEditorState} from "../../../services/query-editor.state";
import {TypesService} from "../../../services/types.service";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {CompilationMessage, Schema} from "../../../services/schema";
import {NgIf} from "@angular/common";
import {TuiButtonModule} from "@taiga-ui/core";

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
    TuiButtonModule
  ],
  template: `
    <app-panel-header title="Query editor" [isSecondary]="true">
      <div class="spacer"></div>
      <button tuiButton size="s" appearance="primary"
              class='button-small menu-bar-button'
              *ngIf="queryState.currentState() !== 'Running' && queryState.currentState() !== 'Cancelling'"
              (click)='runQuery()'>
        <img src="assets/img/tabler/player-play.svg" class='filter-white'>
        Run
      </button>

    </app-panel-header>
    <as-split direction="vertical">
      <as-split-area>
        <app-code-editor
          [content]="queryState.query()"
          (contentChange)="updateQuery($event)"
          [showCompilationErrors]="false"
          (compilationMessagesUpdated)="compilationMessagesUpdated.emit($event)"
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

  queryEditorState: QueryEditorState;
  schema: Schema

  @Output()
  compilationMessagesUpdated = new EventEmitter<CompilationMessage[]>();

  get queryState():QueryEditorPayload {
    return this.queryEditorState?.payload
  }


  constructor(stateStore: QueryEditorStoreService,
              private typeService: TypesService,
              ) {
    this.queryEditorState = stateStore.createTemporaryQuery()
    typeService.getTypes()
      .pipe(takeUntilDestroyed())
      .subscribe(schema => {
        this.schema = schema;
      });
  }

  runQuery() {
    this.queryEditorState.submitQuery("TaxiQL", this.schema);
  }

  updateQuery($event: string) {
    this.queryState.query.set($event);
  }
}
