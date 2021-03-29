import {Component, Input} from '@angular/core';
import {QueryResult} from '../../../services/query.service';
import {QueryFailure} from '../../query-wizard/query-wizard.component';

@Component({
  selector: 'app-call-explorer',
  templateUrl: './call-explorer.component.html',
  styleUrls: ['./call-explorer.component.scss']
})
export class CallExplorerComponent {

  constructor() {
  }

  selectedChart: 'sequence' | 'graph' = 'sequence';

  @Input()
  queryResult: QueryResult | QueryFailure;

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
