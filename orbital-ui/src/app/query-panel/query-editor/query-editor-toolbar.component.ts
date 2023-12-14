import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {RunningQueryStatus} from '../../services/active-queries-notification-service';
import {Observable} from 'rxjs/internal/Observable';
import {CopyQueryFormat} from 'src/app/query-panel/query-editor/QueryFormatter';
import {AppInfoService, AppConfig} from 'src/app/services/app-info.service';
import { isNullOrUndefined } from 'src/app/utils/utils';


@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-query-editor-toolbar',
    template: `
        <span class='error-message'>{{ error }}</span>
        <tui-select
                *ngIf='config?.featureToggles?.chatGptEnabled'
                tuiTextfieldSize='s'
                [(ngModel)]='queryLanguage'
                (ngModelChange)='queryLanguageChange.emit($event)'
        >
            Query language
            <input
                    tuiTextfield
                    placeholder='Query lanaguage'
            />
            <tui-data-list-wrapper
                    *tuiDataList
                    [items]='queryLanguages'
            ></tui-data-list-wrapper>
        </tui-select>
        <tui-hosted-dropdown
                tuiDropdownAlign="left"
                [content]="copyMenuDropdown"
                [(open)]="copyMenuOpen">
            <button
                    appearance="outline"
                    iconRight="tuiIconChevronDown"
                    class="button-small menu-bar-button"
                    size="s"
                    tuiButton
                    type="button"
                    [pseudoActive]="copyMenuOpen || null"> Copy
            </button>
        </tui-hosted-dropdown>
        <ng-template
                #copyMenuDropdown
                let-close="close"
        >
            <tui-data-list>
                <button tuiOption (click)="copyQuery.emit('query')">Query only</button>
                <button tuiOption (click)="copyQuery.emit('curl')">As cURL statement</button>
                <button tuiOption (click)="copyQuery.emit('snippet')" tuiHint='Available once query has completed'>As
                    code
                </button>
            </tui-data-list>
        </ng-template>

        <tui-hosted-dropdown
                tuiDropdownAlign="left"
                [content]="publishMenuDropdown"
                [(open)]="publishMenuOpen">
            <button
                    appearance="outline"
                    iconRight="tuiIconChevronDown"
                    class="button-small menu-bar-button"
                    size="s"
                    tuiButton
                    type="button"
                    [pseudoActive]="publishMenuOpen || null"> Publish
            </button>
        </tui-hosted-dropdown>
        <ng-template
                #publishMenuDropdown
                let-close="close"
        >
            <tui-data-list>
                <button tuiOption (click)="publishAsHttpEndpoint.emit()">As HTTP Endpoint</button>
<!--                <button tuiOption (click)="copyQuery.emit('curl')">As Data Stream</button>-->
            </tui-data-list>
        </ng-template>
        <button tuiButton size="s" appearance="outline" class="button-small  menu-bar-button"
                (click)="saveClicked.emit()">Save
        </button>
        <button tuiButton size="s" appearance="primary"
                class='button-small menu-bar-button'
                *ngIf="(currentState$ | async) !== 'Running' && (currentState$ | async) !== 'Cancelling'"
                (click)='runQuery()'>
            <img src='assets/img/tabler/player-play.svg' class='filter-white'>
            Run
        </button>
        <div class='running-timer' *ngIf="(currentState$ | async) === 'Generating'">
            <span class='loader'></span>
            <span>Thinking...&nbsp;</span>
        </div>
        <div class='running-timer' *ngIf="(currentState$ | async) === 'Running'">
            <span class='loader'></span>
            <span>Running...&nbsp;</span>
            <app-counter-timer *ngIf='queryStarted' [startDate]='queryStarted'></app-counter-timer>


            <div class='progress'
                 *ngIf="queryStarted && percentComplete > 0 && runningQueryStatus.queryType !== 'STREAMING' && runningQueryStatus.estimatedProjectionCount !== 0">
                <mat-progress-bar mode='determinate' [value]='percentComplete'></mat-progress-bar>
                <span>{{ runningQueryStatus.completedProjections}} of {{ runningQueryStatus.estimatedProjectionCount}}
                    records</span>
            </div>

            <div class='progress'
                 *ngIf="queryStarted && percentComplete > 0  && runningQueryStatus.queryType === 'STREAMING'">
                <mat-progress-bar mode='indeterminate' [value]='percentComplete'></mat-progress-bar>
                <span>{{ runningQueryStatus.completedProjections}}</span>
            </div>

            <button mat-stroked-button *ngIf="(currentState$ | async) === 'Running'" color='accent'
                    (click)='cancelQuery.emit()'
            >Cancel
            </button>
        </div>

        <div class='running-timer' *ngIf="(currentState$ | async) === 'Cancelling'">
            <span class='loader'></span>
            <span>Cancelling...</span>
        </div>
    `,
    styleUrls: ['./query-editor-toolbar.component.scss']
})
export class QueryEditorToolbar {
    config: AppConfig;

    publishMenuOpen = false;
    copyMenuOpen = false;

    onActiveZone(active: boolean): void {
        this.publishMenuOpen = active && this.publishMenuOpen;
    }

    constructor(appInfo: AppInfoService) {
        appInfo.getConfig()
            .subscribe(config => this.config = config);
    }


    queryLanguages: QueryLanguage[] = ['TaxiQL', 'Text'];

    @Input()
    queryLanguage: QueryLanguage = 'TaxiQL';

    @Output()
    queryLanguageChange = new EventEmitter<QueryLanguage>();

    @Output()
    saveClicked = new EventEmitter();

    @Output()
    publishAsHttpEndpoint = new EventEmitter();

    @Input()
    publishAsHttpEndpointEnabled: boolean = false;


    @Input()
    currentState$: Observable<QueryState>;

    @Input()
    error: string;

    @Output()
    copyQuery = new EventEmitter<CopyQueryFormat>();

    @Output()
    executeQuery = new EventEmitter<void>();

    @Input()
    queryStarted: Date;


    @Output()
    cancelQuery = new EventEmitter();

    @Input()
    runningQueryStatus: RunningQueryStatus | null;

    get percentComplete(): number | null {
        if (!this.runningQueryStatus) {
            return null;
        }
        if (isNullOrUndefined(this.runningQueryStatus.estimatedProjectionCount)) {
            return null;
        }
        return (this.runningQueryStatus.completedProjections / this.runningQueryStatus.estimatedProjectionCount) * 100;
    }

    runQuery() {
        this.queryStarted = new Date();
        this.executeQuery.emit();
    }

}

export type QueryLanguage = 'TaxiQL' | 'Text';
export type QueryState = 'Editing' | 'Generating' | 'Running' | 'Result' | 'Error' | 'Cancelling';
