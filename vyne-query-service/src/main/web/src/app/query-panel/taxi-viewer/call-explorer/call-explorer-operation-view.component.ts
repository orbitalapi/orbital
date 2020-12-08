import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import * as HttpStatus from 'http-status-codes';

@Component({
  selector: 'app-call-explorer-operation-view',
  templateUrl: './call-explorer-operation-view.component.html',
  styleUrls: ['./call-explorer-operation-view.component.scss']
})
export class CallExplorerOperationViewComponent {

  @Input()
  operation: any;

  @Output()
  close = new EventEmitter<void>();

  get statusText(): string {
    return HttpStatus.getStatusText(this.operation.resultCode);
  }

  get statusTextClass(): string {
    const codeStart = this.operation.resultCode.toString().substr(0, 1);
    switch (codeStart) {
      case '2' :
        return 'status-success';
      case '3' :
        return 'status-success';
      case '4' :
        return 'status-error';
      case '5' :
        return 'status-error';
    }
  }


  closeClicked() {
    this.close.emit();
  }
}
