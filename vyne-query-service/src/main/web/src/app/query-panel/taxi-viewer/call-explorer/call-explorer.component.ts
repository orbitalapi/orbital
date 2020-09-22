import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-call-explorer',
  templateUrl: './call-explorer.component.html',
  styleUrls: ['./call-explorer.component.scss']
})
export class CallExplorerComponent {

  constructor() {
  }
  selectedChart: 'sequence' | 'graph' = 'graph';

  @Input()
  queryResult: any; // TODO : Grab the types

  selectedOperation: any;

  @Output() downloadRemoteCallsClicked = new EventEmitter<Boolean>();

  getPathOnly(address: string) {
    // Hack - there's proabably a better way
    const parts: string[] = address.split('/');
    return '/' + parts.slice(3).join('/');
  }

  selectOperation(operation) {
    this.selectedOperation = operation;
  }

  downloadRemoteCalls($event: MouseEvent) {
    this.downloadRemoteCallsClicked.emit();
  }
}
