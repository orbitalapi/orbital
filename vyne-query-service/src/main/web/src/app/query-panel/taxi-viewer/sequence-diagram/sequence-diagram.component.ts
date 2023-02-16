import { Component, Input } from '@angular/core';
import { QueryProfileData } from '../../../services/query.service';
import { Observable } from 'rxjs/index';

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
            case a.startTime.getTime() < b.startTime.getTime() :
              return -1;
            case a.startTime.getTime() > b.startTime.getTime() :
              return 1;
            default:
              return 0;
          }
        });
      const remoteCallLines = sortedRemoteCalls
        .map(remoteCall => {
          const wasSuccessful = remoteCall.success
          const resultMessage = (wasSuccessful) ? `${remoteCall.responseTypeDisplayName} (${remoteCall.durationMs}ms)`
            : `${remoteCall.resultCode} (${remoteCall.durationMs}ms)`
          const indent = '    ';
          const lines = [indent + `Orbital ->> ${remoteCall.serviceDisplayName}: ${remoteCall.operationName} (${remoteCall.method})`,
            indent + `${remoteCall.serviceDisplayName} ->> Orbital: ${resultMessage}`
          ].join('\n');
          return lines;

        }).join('\n');

      this.remoteCallMermaid = 'sequenceDiagram\n' + remoteCallLines;
    });


  }


}
