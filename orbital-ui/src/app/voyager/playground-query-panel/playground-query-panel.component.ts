import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BehaviorSubject, EMPTY, switchMap} from "rxjs";
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {TuiAccordionModule, TuiTabsModule} from "@taiga-ui/kit";
import {TuiButtonModule} from "@taiga-ui/core";
import {AngularSplitModule} from "angular-split";
import {CodeEditorModule} from "../../code-editor/code-editor.module";
import {StubPanelComponent} from "./stub-panel.component";
import {Schema} from "../../services/schema";
import {HttpClientModule} from "@angular/common/http";
import {VoyagerService} from "../../../voyager-app/voyager.service";
import {emptyQueryMessage, QueryParseMetadata, StubQueryMessage} from "../../services/query.service";
import {JsonViewerModule} from "../../json-viewer/json-viewer.module";
import {QueryConfigPanelComponent} from "./query-config-panel.component";
import {catchError, debounceTime, filter, tap} from "rxjs/operators";
import {QueryPlanPanelComponent} from "./query-plan-panel.component";
import {ExpandablePanelComponent} from "../../expanding-panelset/expandable-panel/expandable-panel.component";
import {QueryResultsPanelComponent} from "./query-results-panel.component";

@Component({
  selector: 'app-playground-query-panel',
  standalone: true,
  imports: [CommonModule, ExpandingPanelSetModule, TuiAccordionModule, TuiButtonModule, AngularSplitModule, TuiTabsModule, CodeEditorModule, StubPanelComponent, HttpClientModule, JsonViewerModule, QueryConfigPanelComponent, QueryPlanPanelComponent, ExpandablePanelComponent, QueryResultsPanelComponent],
  template: `
    <as-split direction="vertical" unit="percent" gutterSize="1">
      <div class="thin-splitter" *asSplitGutter="let isDragged = isDragged" [class.dragged]="isDragged">
        <div class="thin-splitter-gutter-icon"></div>
      </div>
      <as-split-area size="50">
        <app-panel-header title="Query">
          <span class="spacer"></span>
          <button tuiButton size="s" appearance="primary"
                  class='button-small menu-bar-button'
                  (click)='runQuery()'>
            <img src='assets/img/tabler/player-play.svg' class='filter-white'>
            Run
          </button>

        </app-panel-header>
        <app-code-editor
          [content]="content.getValue()" (contentChange)="content.next($event)"
          wordWrap="on"
        >
        </app-code-editor>
      </as-split-area>
      <as-split-area size="50">
        <tui-accordion [rounded]="false">
          <tui-accordion-item size="s">
            Query plan
            <ng-template tuiAccordionItemContent>
              <app-query-plan-panel [(expanded)]="queryPlanExpanded"
                                    [queryPlan]="parsedQuery?.queryPlan"></app-query-plan-panel>
            </ng-template>
          </tui-accordion-item>
          <tui-accordion-item size="s">
            Stubs and parameters
            <ng-template tuiAccordionItemContent>
              <app-query-config-panel [(expanded)]="configPanelExpanded" [(stubs)]="queryMessage.stubs"
                                      [parameters]="queryMessage.parameters"
                                      (parameterValuesChange)="updateQueryParameters($event)"
                                      [schema]="schema"></app-query-config-panel>
            </ng-template>
          </tui-accordion-item>
          <tui-accordion-item size="s">
            Results
            <ng-template tuiAccordionItemContent>
              <div class="result-panel">
                <app-json-viewer [readOnly]="true" [json]="queryResult" [showHeader]="false"
                                 *ngIf="queryResult"></app-json-viewer>
                <div *ngIf="!queryResult" class="empty-results">
                  No results to show
                </div>
              </div>

            </ng-template>
          </tui-accordion-item>
        </tui-accordion>


<!--        <as-split direction="vertical" unit="pixel" gutterSize="1" [useTransition]="true">-->
<!--          <div class="thin-splitter" *asSplitGutter="let isDragged = isDragged" [class.dragged]="isDragged">-->
<!--            <div class="thin-splitter-gutter-icon"></div>-->
<!--          </div>-->
<!--          <as-split-area [lockSize]="!queryPlanExpanded" [size]="queryPlanExpanded ? 280 : 50">-->

<!--          </as-split-area>-->
<!--          <as-split-area [lockSize]="!configPanelExpanded" [size]="configPanelExpanded ? 280 : 50">-->
<!--            <app-query-config-panel [(expanded)]="configPanelExpanded" [(stubs)]="queryMessage.stubs"-->
<!--                                    [parameters]="queryMessage.parameters"-->
<!--                                    (parameterValuesChange)="updateQueryParameters($event)"-->
<!--                                    [schema]="schema"></app-query-config-panel>-->
<!--          </as-split-area>-->
<!--          <as-split-area [size]="configPanelExpanded ? '*' : 50">-->
<!--            <app-query-results-panel>-->

<!--            </app-query-results-panel>-->
<!--            <div class="result-panel">-->
<!--              <app-expandable-panel title="Results" [isSecondary]="true">-->
<!--                <app-json-viewer [readOnly]="true" [json]="queryResult" [showHeader]="false"-->
<!--                                 *ngIf="queryResult"></app-json-viewer>-->
<!--                <div *ngIf="!queryResult" class="empty-results">-->
<!--                  No results to show-->
<!--                </div>-->
<!--              </app-expandable-panel>-->
<!--            </div>-->
<!--          </as-split-area>-->
<!--        </as-split>-->
      </as-split-area>

    </as-split>
  `,
  styleUrls: ['./playground-query-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlaygroundQueryPanelComponent {

  content = new BehaviorSubject<string>(`query HelloWorld(name: String) { \n find { 'Hello ' + name } \n}`)

  constructor(private service: VoyagerService, private changeDetector: ChangeDetectorRef) {
    this.content
      .pipe(
        tap(querySrc => this.queryMessage.query = querySrc),
        debounceTime(500),
        filter(query => {
          return this.queryMessage && this.queryMessage.query.length > 0;
        }),
        switchMap(query => {
          return this.service.parseQuery(this.queryMessage)
            .pipe(catchError(e => {
              return EMPTY;
            }))
            ;
        })
      )
      .subscribe(
        {
          next: (value) => {
            this.parsedQuery = value;
            this.updateQueryParametersFromServer(this.parsedQuery);
            this.changeDetector.markForCheck();
          }
        }
      )
  }

  queryPlanExpanded: boolean = true;
  queryResultsExpanded: boolean = false;
  configPanelExpanded: boolean = false;

  @Input()
  schema: Schema

  parsedQuery: QueryParseMetadata = null;

  queryResult: string | null = null;


  private _queryMessage: StubQueryMessage = emptyQueryMessage();
  @Input()
  get queryMessage(): StubQueryMessage {
    return this._queryMessage;
  }

  set queryMessage(value: StubQueryMessage) {
    this._queryMessage = value;
    this.content.next(this.queryMessage.query || '');
  }

  runQuery() {
    this.service.runQuery(this.queryMessage)
      .subscribe({
        next: result => {
          this.queryResult = JSON.stringify(result, null, 3)
          this.changeDetector.markForCheck();
        },
        error: err => {
          this.queryResult = JSON.stringify(err.error, null, 3);
          this.changeDetector.markForCheck();
        }
      })
  }

  updateQueryParameters(updatedParams: { [index: string]: any }) {
    this.queryMessage.parameters = {}
    Object.keys(updatedParams).forEach(key => {
      this.queryMessage.parameters[key] = updatedParams[key]
    })
    this.changeDetector.markForCheck();
  }

  private updateQueryParametersFromServer(parsedQuery: QueryParseMetadata) {
    const paramKeys: Set<string> = new Set(parsedQuery.parameters.map(p => p.name));
    // Add missing keys from paramKeys to parameters
    for (const key of paramKeys) {
      if (!(key in this.queryMessage.parameters)) {
        this.queryMessage.parameters[key] = null; // Or set a default value as required
      }
    }

    // Remove keys from parameters that are not present in paramKeys
    for (const key in this.queryMessage.parameters) {
      if (!paramKeys.has(key)) {
        delete this.queryMessage.parameters[key];
      }
    }

  }
}
