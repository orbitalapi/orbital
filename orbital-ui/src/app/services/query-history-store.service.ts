import {QueryHistorySummary, QueryService} from './query.service';
import {VyneServicesModule} from './vyne-services.module';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpRequestState, httpRequestStates} from 'ngx-http-request-state';

@Injectable({
  providedIn: VyneServicesModule
})
export class QueryHistoryStoreService {

  private _history$: Observable<HttpRequestState<QueryHistorySummary[]>>;
  constructor(private queryService: QueryService) {
  }

  get history$(): Observable<HttpRequestState<QueryHistorySummary[]>> {
    return this._history$;
  }

  set history$(val) {
    this._history$ = val;
  }

  getHistory() {
    this.history$ = this.queryService.getHistory().pipe(httpRequestStates());
  }
}
