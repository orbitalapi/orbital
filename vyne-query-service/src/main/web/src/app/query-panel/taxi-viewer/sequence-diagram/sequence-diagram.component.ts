import {Component, Input, OnInit} from '@angular/core';
import {QueryProfileData, QueryResult} from '../../../services/query.service';
import { Observable } from 'rxjs/index';
import { QualifiedName } from '../../../services/schema';

@Component({
  selector: 'app-sequence-diagram',
  templateUrl: './sequence-diagram.component.html',
  styleUrls: ['./sequence-diagram.component.scss']
})
export class SequenceDiagramComponent {
  remoteCallMermaid: string;

  private _profileData$: Observable<QueryProfileData>;

  @Input()
  set profileData$(value: Observable<QueryProfileData>) {
    if (this._profileData$ === value) {
      return;
    }
    this._profileData$ = value;
    this.generateRemoteCallMermaid();
  }

  get profileData$(): Observable<QueryProfileData> {
    return this._profileData$;
  }

  private generateRemoteCallMermaid() {
    if (!this._profileData$) {
      this.remoteCallMermaid = '';
    }
    this.profileData$.subscribe(profileData => {
      const sortedRemoteCalls = profileData.remoteCalls
        .sort((a, b) => {
          switch (true) {
            case a.timestamp.getTime() < b.timestamp.getTime() :
              return -1;
            case a.timestamp.getTime() > b.timestamp.getTime() :
              return 1;
            default:
              return 0;
          }
        });
      const remoteCallLines = sortedRemoteCalls
        .map(remoteCall => {
          const wasSuccessful = remoteCall.resultCode >= 200 && remoteCall.resultCode <= 299;
          let resultMessage = wasSuccessful ? 'Success ' : 'Error ';
          resultMessage += remoteCall.resultCode;
          const indent = '    ';
          const lines = [indent + `Vyne ->> ${remoteCall.serviceDisplayName || remoteCall.service}: ${remoteCall.operation} (${remoteCall.method})`,
            indent + `${remoteCall.serviceDisplayName || remoteCall.service} ->> Vyne: ${remoteCall.responseTypeDisplayName || remoteCall.responseTypeName} (${remoteCall.durationMs}ms)`
          ].join('\n');
          return lines;

        }).join('\n');

      this.remoteCallMermaid = 'sequenceDiagram\n' + remoteCallLines;
    });


  }


}
