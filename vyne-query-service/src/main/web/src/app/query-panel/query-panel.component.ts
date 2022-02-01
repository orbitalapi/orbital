import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {QueryHistorySummary, QueryResult, QueryService} from '../services/query.service';
import {QueryFailure} from './query-wizard/query-wizard.component';
import {isQueryResult} from './result-display/BaseQueryResultComponent';
import {BaseQueryResultDisplayComponent} from './BaseQueryResultDisplayComponent';
import {TypesService} from '../services/types.service';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'query-panel',
  templateUrl: './query-panel.component.html',
  styleUrls: ['./query-panel.component.scss'],
})
export class QueryPanelComponent extends BaseQueryResultDisplayComponent {
  routeQuery: QueryHistorySummary;
  selectedIndex: number;

  lastQueryResult: QueryResult | QueryFailure;

  loading = false;

  links: { label: string, link: string }[] = [
    { label: 'Query Builder', link: 'builder'},
    { label: 'Query Editor', link: 'editor'},
  ];

  constructor(private router: Router, queryService: QueryService, typeService: TypesService, activatedRoute: ActivatedRoute) {
    super(queryService, typeService);
    // https://angular.io/api/router/NavigationExtras#state
    const navigationState = this.router.getCurrentNavigation() && this.router.getCurrentNavigation().extras
      ? this.router.getCurrentNavigation().extras.state
      : null;
    this.routeQuery = navigationState ? navigationState.query : undefined;
    this.selectedIndex = this.routeQuery ? (!isNullOrUndefined(this.routeQuery.taxiQl) ? 1 : 0) : 0;
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
