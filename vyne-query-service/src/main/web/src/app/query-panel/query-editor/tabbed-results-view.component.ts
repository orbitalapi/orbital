import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {BaseQueryResultComponent} from '../result-display/BaseQueryResultComponent';
import {TypesService} from '../../services/types.service';
import {DownloadClickedEvent} from '../../object-view/object-view-container.component';
import {Observable} from 'rxjs/index';
import {InstanceLike, Type} from '../../services/schema';
import {QueryProfileData} from '../../services/query.service';

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
            <!--            <div class="empty-results" *ngIf="queryResultTypeNames.length === 0">-->
            <!--              <img src="assets/img/no-results.svg">-->
            <!--              <p>There's nothing to display here.</p>-->
            <!--            </div>-->
            <div class="results-object-view-list-block">
              <app-object-view-container [instances$]="instances$"
                                         [schema]="schema"
                                         [selectable]="true"
                                         [downloadSupported]="true"
                                         (downloadClicked)="this.downloadClicked.emit($event)"
                                         [type]="type"
                                         [anonymousTypes]="anonymousTypes"
                                         (instanceClicked)="instanceClicked($event,type.name)"></app-object-view-container>
            </div>
          </div>
        </ng-template>
      </mat-tab>
      <mat-tab label="Profiler" [disabled]="!profileData$">
        <ng-template matTabContent>
          <app-call-explorer [queryProfileData$]="profileData$"></app-call-explorer>
        </ng-template>
      </mat-tab>
    </mat-tab-group>
  `,
  styleUrls: ['./tabbed-results-view.component.scss']
})
export class TabbedResultsViewComponent extends BaseQueryResultComponent {

  constructor(protected typeService: TypesService) {
    super(typeService);
  }

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
