import {Component, Input, OnInit} from '@angular/core';
import {QueryResult} from '../../../services/query.service';

@Component({
  selector: 'app-sequence-diagram',
  templateUrl: './sequence-diagram.component.html',
  styleUrls: ['./sequence-diagram.component.scss']
})
export class SequenceDiagramComponent {
  remoteCallMermaid: string;

  private _result: QueryResult;

  @Input()
  set result(value: QueryResult) {
    this._result = value;
    this.generateRemoteCallMermaid();
  }

  get result(): QueryResult {
    return this._result;
  }

  private generateRemoteCallMermaid() {
    if (!this._result || this._result.remoteCalls.length === 0) {
      this.remoteCallMermaid = '';
    }

    const remoteCallLines = this._result.remoteCalls.map(remoteCall => {
      const wasSuccessful = remoteCall.resultCode >= 200 && remoteCall.resultCode <= 299;
      let resultMessage = wasSuccessful ? 'Success ' : 'Error ';
      resultMessage += remoteCall.resultCode;
      const indent = '    ';
      const lines = [indent + `Vyne ->> ${remoteCall.service}: ${remoteCall.operation} (${remoteCall.method})`,
        indent + `${remoteCall.service} ->> Vyne: ${remoteCall.responseTypeName} (${remoteCall.durationMs}ms)`
      ].join('\n');
      return lines;

    }).join('\n');

    this.remoteCallMermaid = 'sequenceDiagram\n' + remoteCallLines;
  }


}
