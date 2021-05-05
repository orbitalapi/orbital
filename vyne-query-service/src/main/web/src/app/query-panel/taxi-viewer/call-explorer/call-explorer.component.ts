import {Component, Input} from '@angular/core';
import {QueryProfileData, QueryResult, RemoteCall} from '../../../services/query.service';
import {QueryFailure} from '../../query-wizard/query-wizard.component';
import {Observable} from 'rxjs/index';
import {map} from 'rxjs/operators';

@Component({
  selector: 'app-call-explorer',
  templateUrl: './call-explorer.component.html',
  styleUrls: ['./call-explorer.component.scss']
})
export class CallExplorerComponent {

  constructor() {
  }

  selectedChart: 'sequence' | 'graph' = 'sequence';

  private _queryProfileData$: Observable<QueryProfileData>;

  remoteCalls$: Observable<RemoteCall[]>;

  @Input()
  get queryProfileData$(): Observable<QueryProfileData> {
    return this._queryProfileData$;
  }

  set queryProfileData$(value: Observable<QueryProfileData>) {
    if (this._queryProfileData$ === value) {
      return;
    }
    this._queryProfileData$ = value;
    this.remoteCalls$ = value.pipe(map(queryProfileData => queryProfileData.remoteCalls));
  }


  selectedOperation: any;

  getPathOnly(address: string) {
    // Hack - there's proabably a better way
    const parts: string[] = address.split('/');
    return '/' + parts.slice(3).join('/');
  }

  selectOperation(operation) {
    this.selectedOperation = operation;
  }
}
