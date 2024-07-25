import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component, DestroyRef,
  ElementRef,
  Input,
  OnInit,
  ViewChild
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BehaviorSubject, EMPTY, switchMap} from "rxjs";
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {TuiAccordionModule, TuiBadgeModule, TuiTabsModule} from "@taiga-ui/kit";
import {TuiButtonModule} from "@taiga-ui/core";
import {AngularSplitModule, IOutputData} from "angular-split";
import {CodeEditorModule} from "../../code-editor/code-editor.module";
import {StubPanelComponent} from "./stub-panel.component";
import {Schema} from "../../services/schema";
import {HttpClientModule} from "@angular/common/http";
import {VoyagerService} from "../../../voyager-app/voyager.service";
import {emptyQueryMessage, QueryParseMetadata, StubQueryMessage} from "../../services/query.service";
import {JsonViewerModule} from "../../json-viewer/json-viewer.module";
import {QueryConfigPanelComponent} from "./query-config-panel.component";
import {catchError, debounceTime, filter, tap} from "rxjs/operators";
import {ExpandablePanelComponent} from "../../expanding-panelset/expandable-panel/expandable-panel.component";
import {QueryResultsPanelComponent} from "./query-results-panel.component";
import {ResizeObservableService} from "../../services/resize-observable.service";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {LineageDisplayModule} from "../../lineage-display/lineage-display.module";
import {TuiChipModule} from "@taiga-ui/experimental";

@Component({
  selector: 'app-playground-query-panel',
  standalone: true,
  providers: [ResizeObservableService],
  imports: [CommonModule, ExpandingPanelSetModule, TuiAccordionModule, TuiButtonModule, AngularSplitModule, TuiTabsModule, CodeEditorModule, StubPanelComponent, HttpClientModule, JsonViewerModule, QueryConfigPanelComponent, ExpandablePanelComponent, QueryResultsPanelComponent, LineageDisplayModule, TuiBadgeModule, TuiChipModule],
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
        <tui-accordion [rounded]="false" #accordion>
          <tui-accordion-item size="s" [(open)]="queryPlanExpanded">
            Query plan
            <ng-template tuiAccordionItemContent>
              <app-query-lineage [(fullscreen)]="queryPlanFullscreen" *ngIf="!queryPlanFullscreen"
                                 [style.height]="expandedPanelHeight"
                                 [rows]="parsedQuery?.queryPlan?.steps"></app-query-lineage>
            </ng-template>
          </tui-accordion-item>
          <tui-accordion-item size="s" [(open)]="configPanelExpanded">
            <div class="stubs-params-header">
                <span>Stubs and parameters</span>
                <tui-chip appearance="info" *ngIf="queryMessage.stubs?.length > 0">{{ queryMessage.stubs.length | i18nPlural: stubsPluralMap}}</tui-chip>
                <tui-chip appearance="info" *ngIf="queryMessage.parameters?.length > 0">{{ queryMessage.parameters.length | i18nPlural: paramsPluralMap }} defined</tui-chip>
            </div>

            <ng-template tuiAccordionItemContent>
              <app-query-config-panel [style.height]="expandedPanelHeight"
                                      [(stubs)]="queryMessage.stubs"
                                      [parameters]="queryMessage.parameters"
                                      (parameterValuesChange)="updateQueryParameters($event)"
                                      [schema]="schema"></app-query-config-panel>
            </ng-template>
          </tui-accordion-item>
          <tui-accordion-item size="s" [(open)]="queryResultsExpanded">
            Results
            <ng-template tuiAccordionItemContent>
              <div class="result-panel" [style.height]="expandedPanelHeight">
                <app-json-viewer [readOnly]="true" [json]="queryResult" [showHeader]="false"
                                 *ngIf="queryResult"></app-json-viewer>
                <div *ngIf="!queryResult" class="empty-results">
                  No results to show
                </div>
              </div>

            </ng-template>
          </tui-accordion-item>
        </tui-accordion>
      </as-split-area>

    </as-split>
    <!-- This is a hacky workaround.
     See comments on QueryLineageComponent fullscreen as to why we have have two app-query-lineage
     components (one inside the accordion, and one outside)
     -->
    <app-query-lineage *ngIf="queryPlanFullscreen" [(fullscreen)]="queryPlanFullscreen"
                       [rows]="parsedQuery?.queryPlan?.steps"></app-query-lineage>
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
    this.resizeObservableService.resizeObservable(this.accordionElement.nativeElement)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(resizeEntry => {
        this.updatePanelSizes();
      })
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


  private _queryMessage: StubQueryMessage = emptyQueryMessage();
  queryPlanFullscreen: boolean = false;

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

  updatePanelSizes() {
    const accordionHeight = this.accordionElement.nativeElement.clientHeight;
    const accordionHeaderHeight = this.accordionElement.nativeElement.querySelector('button.t-header').clientHeight;
    const totalHeaderHeight = accordionHeaderHeight * 3
    this.expandedPanelHeight = `${accordionHeight - totalHeaderHeight}px`
    this.changeDetector.markForCheck();
  }
}
