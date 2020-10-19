import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {BaseQueryResultComponent} from '../result-display/BaseQueryResultComponent';
import {TypesService} from '../../services/types.service';
import {DownloadFileType} from '../result-display/result-container.component';
import {DownloadClickedEvent} from '../../object-view/object-view-container.component';

@Component({
  selector: 'app-tabbed-results-view',
  template: `
    <mat-tab-group mat-align-tabs="start" style="height: 100%">
      <mat-tab label="Query results">
        <ng-template matTabContent>
          <div class="results-container">
            <div class="empty-results" *ngIf="queryResultTypeNames.length === 0">
              <img src="assets/img/no-results.svg">
              <p>There's nothing to display here.</p>
            </div>
            <div *ngFor="let resultTypeName of queryResultTypeNames">
              <app-object-view-container [instance]="getResultForTypeName(resultTypeName)"
                                         [schema]="schema"
                                         [selectable]="true"
                                         [downloadSupported]="true"
                                         (downloadClicked)="this.downloadClicked.emit($event)"
                                         [type]="getTypeIfNotIncluded(resultTypeName)"
                                         (instanceClicked)="instanceClicked($event)"></app-object-view-container>
            </div>
          </div>
        </ng-template>
      </mat-tab>
      <mat-tab label="Profiler">
        <ng-template matTabContent>
          <app-call-explorer [queryResult]="result"></app-call-explorer>
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

  @Output()
  downloadClicked = new EventEmitter<DownloadClickedEvent>();

  protected updateDataSources() {
  }

}
