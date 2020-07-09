import {Component} from '@angular/core';
import { Router} from '@angular/router';
import { QueryHistoryRecord, isVyneQlQueryHistoryRecord } from '../services/query.service';
import { VyneQueryViewerComponent } from './taxi-viewer/vyne-query-viewer/vyne-query-viewer.component';
import { VyneqlRecordComponent } from '../query-history/vyneql-record.component';

@Component({
  selector: 'query-panel',
  templateUrl: './query-panel.component.html',
  styleUrls: ['./query-panel.component.scss'],
})
export class QueryPanelComponent  {

  routeQuery: QueryHistoryRecord

  constructor(private router: Router) {
    // https://angular.io/api/router/NavigationExtras#state
    const navigationState = this.router.getCurrentNavigation().extras.state
    this.routeQuery =  navigationState ? navigationState.query : undefined
  }

  activeTab() {
    return this.routeQuery ? (isVyneQlQueryHistoryRecord(this.routeQuery) ? 1: 0) : 0
  }

 

}
