import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {
  QueryHistoryRecord,
  isVyneQlQueryHistoryRecord,
  QueryResult,
  DataSource,
  QueryService
} from '../services/query.service';
import {VyneQueryViewerComponent} from './taxi-viewer/vyne-query-viewer/vyne-query-viewer.component';
import {VyneqlRecordComponent} from '../query-history/vyneql-record.component';
import {QueryFailure} from './query-wizard/query-wizard.component';
import {InstanceLike} from '../object-view/object-view.component';
import {Type} from '../services/schema';
import {InstanceSelectedEvent} from './result-display/result-container.component';
import {isQueryResult, QueryResultInstanceSelectedEvent} from './result-display/BaseQueryResultComponent';
import {BaseQueryResultDisplayComponent} from './BaseQueryResultDisplayComponent';
import {TypesService} from '../services/types.service';

@Component({
  selector: 'query-panel',
  templateUrl: './query-panel.component.html',
  styleUrls: ['./query-panel.component.scss'],
})
export class QueryPanelComponent extends BaseQueryResultDisplayComponent {
  routeQuery: QueryHistoryRecord;
  selectedIndex: number;

  lastQueryResult: QueryResult | QueryFailure;

  loading = false;

  constructor(private router: Router, queryService: QueryService, typeService: TypesService) {
    super(queryService, typeService);
    // https://angular.io/api/router/NavigationExtras#state
    const navigationState = this.router.getCurrentNavigation().extras.state;
    this.routeQuery = navigationState ? navigationState.query : undefined;
    this.selectedIndex = this.routeQuery ? (isVyneQlQueryHistoryRecord(this.routeQuery) ? 1 : 0) : 0;
  }

  get queryId(): string {
    if (isQueryResult(this.lastQueryResult)) {
      return this.lastQueryResult.queryResponseId;
    } else {
      return null;
    }
  }

  resultUpdated(result: QueryResult | QueryFailure) {
    this.lastQueryResult = result;
  }

  onLoadingChanged($event: boolean) {
    this.loading = $event;
  }

  onCloseTypedInstanceDrawer($event: boolean) {
    this.shouldTypedInstancePanelBeVisible = $event;
  }
}
