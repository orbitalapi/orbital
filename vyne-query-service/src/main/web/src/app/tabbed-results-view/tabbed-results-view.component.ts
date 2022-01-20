import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Observable} from 'rxjs';
import {DownloadClickedEvent} from '../object-view/object-view-container.component';
import {InstanceLike, Type} from '../services/schema';
import {QueryProfileData} from '../services/query.service';
import {BaseQueryResultComponent} from '../query-panel/result-display/BaseQueryResultComponent';
import {TypesService} from '../services/types.service';
import {AppInfoService, QueryServiceConfig} from '../services/app-info.service';

@Component({
  selector: 'app-tabbed-results-view',
  template: `
      <!--    <app-error-panel *ngIf="lastQueryResultAsSuccess?.unmatchedNodes?.length > 0"-->
      <!--                     [queryResult]="lastQueryResultAsSuccess">-->
      <!--    </app-error-panel>-->
      <mat-tab-group mat-align-tabs="start" style="height: 100%" animationDuration="150ms">
          <mat-tab label="Query results">
              <ng-template matTabContent>
                  <div class="results-container">
                      <div class="results-object-view-list-block">
                          <app-object-view-container [instances$]="instances$"
                                                     [schema]="schema"
                                                     [selectable]="true"
                                                     [downloadSupported]="downloadSupported"
                                                     (downloadClicked)="this.downloadClicked.emit($event)"
                                                     [type]="type"
                                                     [anonymousTypes]="anonymousTypes"
                                                     (instanceClicked)="instanceClicked($event,type.name)"></app-object-view-container>
                      </div>
                  </div>
              </ng-template>
          </mat-tab>
          <mat-tab label="Profiler" *ngIf="profileData$ && config && config.analytics.persistResults">
              <ng-template matTabContent>
                  <app-call-explorer [queryProfileData$]="profileData$"></app-call-explorer>
              </ng-template>
          </mat-tab>
      </mat-tab-group>
  `,
  styleUrls: ['./tabbed-results-view.component.scss']
})
export class TabbedResultsViewComponent extends BaseQueryResultComponent {
  config: QueryServiceConfig;

  constructor(protected typeService: TypesService, protected appInfoService: AppInfoService) {
    super(typeService);
    appInfoService.getConfig()
      .subscribe(next => this.config = next);
  }

  @Input()
  downloadSupported = true;

  @Input()
  instances$: Observable<InstanceLike>;

  @Input()
  type: Type;

  @Input()
  anonymousTypes: Type[] = [];

  @Output()
  downloadClicked = new EventEmitter<DownloadClickedEvent>();

  @Input()
  profileData$: Observable<QueryProfileData>;

  protected updateDataSources() {
  }


}
