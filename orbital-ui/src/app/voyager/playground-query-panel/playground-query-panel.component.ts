import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component, DestroyRef,
  ElementRef,
  Input,
  ViewChild
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BehaviorSubject, EMPTY, switchMap} from "rxjs";
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import { TuiAccordion, TuiBadge, TuiTabs, TuiChip } from "@taiga-ui/kit";
import {TuiExpand, TuiButton, TuiNotification} from "@taiga-ui/core";
import {AngularSplitModule, IOutputData} from "angular-split";
import {CodeEditorModule} from "../../code-editor/code-editor.module";
import {StubPanelComponent} from "./stub-panel.component";
import {Schema} from "../../services/schema";
import {HttpClientModule} from "@angular/common/http";
import {VoyagerService} from "../../../voyager-app/voyager.service";
import {emptyQueryMessage, QueryParseMetadata, QueryProfileData, StubQueryMessage} from "../../services/query.service";
import {JsonViewerModule} from "../../json-viewer/json-viewer.module";
import {QueryConfigPanelComponent} from "./query-config-panel.component";
import {catchError, debounceTime, filter, tap} from "rxjs/operators";
import {ExpandablePanelComponent} from "../../expanding-panelset/expandable-panel/expandable-panel.component";
import {QueryResultsPanelComponent} from "./query-results-panel.component";
import {ResizeObservableService} from "../../services/resize-observable.service";
import {LineageDisplayModule} from "../../lineage-display/lineage-display.module";
import {isNullOrUndefined} from "../../utils/utils";

@Component({
  selector: 'app-playground-query-panel',
  standalone: true,
  providers: [ResizeObservableService],
  imports: [CommonModule, ExpandingPanelSetModule, TuiAccordion, TuiButton, AngularSplitModule, TuiTabs, CodeEditorModule, StubPanelComponent, HttpClientModule, JsonViewerModule, QueryConfigPanelComponent, ExpandablePanelComponent, QueryResultsPanelComponent, LineageDisplayModule, TuiBadge, TuiChip, TuiExpand, TuiNotification],
  template: `
    <as-split direction="vertical" unit="percent" gutterSize="1">
      <div class="thin-splitter" *asSplitGutter="let isDragged = isDragged" [class.dragged]="isDragged">
        <div class="thin-splitter-gutter-icon"></div>
      </div>
      <as-split-area size="50" class="flex-split-area">
        <app-panel-header tablerIcon="file-search" title="Query">
          <span class="spacer"></span>
          <button tuiButton size="s" appearance="primary"
                  class='button-small menu-bar-button'
                  (click)='runQuery()'>
            <img src='assets/img/tabler/player-play.svg' class='filter-white'>
            Run
          </button>

        </app-panel-header>
        <tui-notification *ngIf="usesStubs" size="s">This query has stubs configured</tui-notification>
        <app-code-editor
          wordWrap="on"
          [content]="content.getValue()"
          [showCompilationProblemsPanel]="true"
          (contentChange)="content.next($event)"
          [setFocus]="false"
        >
        </app-code-editor>
      </as-split-area>
      <as-split-area size="50" class="bottom-split-area">
        <app-panel-header [collapsible]="true" [(expanded)]="configPanelExpanded" #stubsPanelHeader
                          [isSecondary]="true" title="Stubs and Parameters">
          <div class="stubs-params-header">
            <tui-badge appearance="info"
                       *ngIf="queryMessage.stubs?.length > 0">{{ queryMessage.stubs.length | i18nPlural: stubsPluralMap }}
            </tui-badge>
            <tui-badge appearance="info"
                       *ngIf="queryMessage.parameters?.length > 0">{{ queryMessage.parameters.length | i18nPlural: paramsPluralMap }}
              defined
            </tui-badge>
          </div>
        </app-panel-header>
        <tui-expand [expanded]="stubsPanelHeader.expanded">
          <app-query-config-panel [style.height]="expandedPanelHeight"
                                  [(stubs)]="queryMessage.stubs"
                                  [parameters]="queryMessage.parameters"
                                  (parameterValuesChange)="updateQueryParameters($event)"
                                  [schema]="schema"></app-query-config-panel>
        </tui-expand>
        <app-query-results-panel
          [(expanded)]="queryResultsExpanded"
          [queryResult]="queryResult"
          [profileData]="queryProfileData"
          [usesStubs]="usesStubs"
        ></app-query-results-panel>
      </as-split-area>

    </as-split>
  `,
  styleUrls: ['./playground-query-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlaygroundQueryPanelComponent implements AfterViewInit {
  stubsPluralMap: {[k: string]: string} = {
    '=1' : '1 service call stubbed',
    'other' : '# service calls stubbed'
  }
  paramsPluralMap: {[k: string]: string} = {
    '=1' : '1 parameter',
    'other' : '# parameters'
  }
  content = new BehaviorSubject<string>(`query HelloWorld(name: String) { \n find { 'Hello ' + name } \n}`)

  constructor(private service: VoyagerService,
              private changeDetector: ChangeDetectorRef,
              private resizeObservableService: ResizeObservableService,
              private destroyRef: DestroyRef
  ) {
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

  ngAfterViewInit(): void {
    this.updatePanelSizes();
  }

  queryPlanExpanded: boolean = true;
  queryResultsExpanded: boolean = false;
  configPanelExpanded: boolean = false;

  expandedPanelHeight: string = '300px';

  @ViewChild('accordion', {read: ElementRef})
  accordionElement: ElementRef;

  @Input()
  schema: Schema

  parsedQuery: QueryParseMetadata = null;

  queryResult: string | null = null;
  queryProfileData: QueryProfileData | null = null;


  private _queryMessage: StubQueryMessage = emptyQueryMessage();

  get usesStubs() {
    return this.queryMessage?.stubs?.length > 0  ?? false;
  }
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
          this.queryResultsExpanded = true;
          this.queryPlanExpanded = false;
          this.configPanelExpanded = false;
          this.queryResult = result.data;
          this.service.getQueryProfileData(result.queryId)
            .subscribe(profile => {
              this.queryProfileData = profile;
              this.changeDetector.markForCheck();
            })
          this.changeDetector.markForCheck();
        },
        error: err => {
          this.queryResultsExpanded = true;
          this.queryPlanExpanded = false;
          this.configPanelExpanded = false;
          if (!isNullOrUndefined(err.error)) {
            // because the service configures the HTTP handler to treat
            // the response as text, we need to reformat the error to make it readable
            this.queryResult = JSON.stringify(JSON.parse(err.error), null, 3);
          } else {
            this.queryResult = JSON.stringify(err)
          }

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

  updatePanelSizes() {
    // const accordionHeight = this.accordionElement.nativeElement.clientHeight;
    // const accordionHeaderHeight = this.accordionElement.nativeElement.querySelector('button.t-header').clientHeight;
    // const totalHeaderHeight = accordionHeaderHeight * 3
    // this.expandedPanelHeight = `${accordionHeight - totalHeaderHeight}px`
    // this.changeDetector.markForCheck();
  }
}
