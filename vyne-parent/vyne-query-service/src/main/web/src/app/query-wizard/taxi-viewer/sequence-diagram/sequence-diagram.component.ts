import {Component, Input, OnInit} from '@angular/core';
import {QueryResult} from "../query.service";

@Component({
  selector: 'app-sequence-diagram',
  templateUrl: './sequence-diagram.component.html',
  styleUrls: ['./sequence-diagram.component.scss']
})
export class SequenceDiagramComponent implements OnInit {
  private _result: QueryResult;

  @Input()
  set result(value: QueryResult) {
    this._result = value;
    this.generateRemoteCallMermaid();
  }
  get result(): QueryResult {
    return this._result;
  }

  constructor() {
  }

  ngOnInit() {
  }

  remoteCallMermaid: string;

  private generateRemoteCallMermaid() {
    if (!this._result || this._result.remoteCalls.length == 0) {
      this.remoteCallMermaid = "";
    }

    let remoteCallLines = this._result.remoteCalls.map(remoteCall => {
      let wasSuccessful = remoteCall.resultCode >= 200 && remoteCall.resultCode <= 299;
      let resultMessage = wasSuccessful ? "Success " : "Error ";
      resultMessage += remoteCall.resultCode;
      let indent = "    ";
      let lines = [indent + `Vyne ->> ${remoteCall.service.name}: ${remoteCall.operation} (${remoteCall.method})`,
        indent + `${remoteCall.service.name} ->> Vyne: ${remoteCall.responseTypeName.name} (${remoteCall.durationMs}ms)`
      ].join("\n");
      return lines;

    }).join("\n");

    this.remoteCallMermaid = "sequenceDiagram\n" + remoteCallLines
  }


}
