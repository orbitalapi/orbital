import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {QueryHistoryRecord, isVyneQlQueryHistoryRecord, QueryResult} from '../services/query.service';
import {VyneQueryViewerComponent} from './taxi-viewer/vyne-query-viewer/vyne-query-viewer.component';
import {VyneqlRecordComponent} from '../query-history/vyneql-record.component';
import {QueryFailure} from './query-wizard/query-wizard.component';
import {InstanceLike} from '../object-view/object-view.component';
import {Type} from '../services/schema';
import {InstanceSelectedEvent} from './result-display/result-container.component';

@Component({
  selector: 'query-panel',
  templateUrl: './query-panel.component.html',
  styleUrls: ['./query-panel.component.scss'],
})
export class QueryPanelComponent {
  routeQuery: QueryHistoryRecord;
  selectedIndex: number;

  lastQueryResult: QueryResult | QueryFailure;

  lineageGraph;

  selectedTypeInstance: InstanceLike;
  selectedTypeInstanceType: Type;

  loading = false;

  get showSidePanel(): boolean {
    return this.selectedTypeInstanceType !== undefined && this.selectedTypeInstance !== null;
  }

  set showSidePanel(value: boolean) {
    if (!value) {
      this.selectedTypeInstance = null;
    }
  }


  constructor(private router: Router) {
    // https://angular.io/api/router/NavigationExtras#state
    const navigationState = this.router.getCurrentNavigation().extras.state;
    this.routeQuery = navigationState ? navigationState.query : undefined;
    this.selectedIndex = this.routeQuery ? (isVyneQlQueryHistoryRecord(this.routeQuery) ? 1 : 0) : 0;
  }


  resultUpdated(result: QueryResult | QueryFailure) {
    this.lastQueryResult = result;
    this.lineageGraph = (result as QueryResult).lineageGraph || null;
  }

  onInstanceSelected($event: InstanceSelectedEvent) {
    this.selectedTypeInstance = $event.selectedTypeInstance;
    this.selectedTypeInstanceType = $event.selectedTypeInstanceType;
  }

  onLoadingChanged($event: boolean) {
    this.loading = $event;
  }
}
