import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {BehaviorSubject, EMPTY, ReplaySubject, switchMap} from "rxjs";
import {ExpandingPanelSetModule} from "../../expanding-panelset/expanding-panel-set.module";
import {TuiAccordionModule, TuiTabsModule} from "@taiga-ui/kit";
import {TuiButtonModule} from "@taiga-ui/core";
import {AngularSplitModule} from "angular-split";
import {CodeEditorModule} from "../../code-editor/code-editor.module";
import {StubPanelComponent} from "./stub-panel.component";
import {Schema} from "../../services/schema";
import {HttpClient, HttpClientModule} from "@angular/common/http";
import {Parameter, TaxiQlQuery, VoyagerService} from "../../../voyager-app/voyager.service";
import {emptyQueryMessage, OperationStub, StubQueryMessage} from "../../services/query.service";
import {JsonViewerModule} from "../../json-viewer/json-viewer.module";
import {QueryConfigPanelComponent} from "./query-config-panel.component";
import {catchError, debounceTime, filter, tap} from "rxjs/operators";

@Component({
  selector: 'app-playground-query-panel',
  standalone: true,
  imports: [CommonModule, ExpandingPanelSetModule, TuiAccordionModule, TuiButtonModule, AngularSplitModule, TuiTabsModule, CodeEditorModule, StubPanelComponent, HttpClientModule, JsonViewerModule, QueryConfigPanelComponent],
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
        <as-split direction="vertical" unit="pixel" gutterSize="1">
          <div class="thin-splitter" *asSplitGutter="let isDragged = isDragged" [class.dragged]="isDragged">
            <div class="thin-splitter-gutter-icon"></div>
          </div>
          <as-split-area [lockSize]="!configPanelExpanded" [size]="configPanelExpanded ? 250 : 50">
            <app-query-config-panel [(expanded)]="configPanelExpanded" [(stubs)]="queryMessage.stubs"
                                    [parameters]="queryMessage.parameters"
                                    (parameterValuesChange)="updateQueryParameters($event)"
                                    [schema]="schema"></app-query-config-panel>
          </as-split-area>
          <as-split-area size="*">
            <div class="result-panel">
              <app-panel-header title="Results"></app-panel-header>
              <app-json-viewer [readOnly]="true" [json]="queryResult" [showHeader]="false"
                               *ngIf="queryResult"></app-json-viewer>
              <div *ngIf="!queryResult" class="empty-results">
                No results to show
              </div>
            </div>


          </as-split-area>
        </as-split>
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

  configPanelExpanded: false;

  @Input()
  schema: Schema

  parsedQuery: TaxiQlQuery = null;

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

  private updateQueryParametersFromServer(parsedQuery: TaxiQlQuery) {
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
