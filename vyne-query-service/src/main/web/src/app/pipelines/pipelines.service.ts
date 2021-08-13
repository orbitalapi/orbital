import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {environment} from '../../environments/environment';
import {Observable} from 'rxjs/internal/Observable';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: VyneServicesModule
})
export class PipelineService {
  constructor(private http: HttpClient) {
  }

  listPipelines(): Observable<PipelineStateSnapshot[]> {
    return this.http.get<PipelineStateSnapshot[]>(`${environment.queryServiceUrl}/api/pipelines`)
      .pipe(map(snapshots => {
        return snapshots.map(snapshot => {
          // pipelineDescription arrives as a string, rather than json
          snapshot.pipelineDescription = JSON.parse(snapshot.pipelineDescription as unknown as string);
          return snapshot;
        });
      }));
  }

  submitPipeline(pipelineSpec: PipelineSpec): Observable<PipelineStateSnapshot> {
    return this.http.post<PipelineStateSnapshot>(`${environment.queryServiceUrl}/api/pipelines`, pipelineSpec);
  }
}

export interface PipelineStateSnapshot {
  name: string;
  pipelineDescription: PipelineSpec;
  state: PipelineState;
  info: string;
}

export interface PipelineRunnerInstance {
  instanceId: string;
  uri: string;
}

export type PipelineState = 'SCHEDULED' | 'STARTING' | 'RUNNING';

export interface PipelineTransport {
  type: PipelineTransportType;
  icon: string;
  label: string;
  description: string;
}

export const PIPELINE_INPUTS: PipelineTransport[] = [
  {
    type: 'httpListener',
    label: 'Http trigger',
    description: 'Expose an HTTP endpoint that when hit, will trigger this pipeline with data',
    icon: ''
  },
  {
    type: 'taxiOperation',
    label: 'Http Poll',
    description: 'Poll a defined HTTP endpoint for data',
    icon: ''
  },
  {
    type: 'kafka',
    label: 'Kafka topic',
    description: 'Subscribe to a kafka topic for messages',
    icon: ''
  }
];

export interface PipelineSpec {
  name: string;
  input: {
    transport: PipelineTransportSpec
  };
  output: {
    transport: PipelineTransportSpec
  };
}

export interface PipelineTransportSpec {
  type: PipelineTransportType;
  direction: PipelineDirection;
}

export const PIPELINE_OUTPUTS: PipelineTransport[] = [
  {
    type: 'kafka',
    label: 'Kafka topic',
    description: 'Publish a message to a kafka topic',
    icon: ''
  },
  {
    type: 'cask',
    label: 'Cask',
    description: 'Store the message in a cask',
    icon: ''
  },
  {
    type: 'taxiOperation',
    label: 'Call an operation',
    description: 'Call an HTTP operation with data',
    icon: ''
  },
];

export interface PollingTaxiOperationInputSpec {
  operationName: string;
  pollSchedule: string;
  parameterMap: any;
}

export type PipelineDirection = 'INPUT' | 'OUTPUT';
export type PipelineTransportType = 'httpListener' | 'taxiOperation' | 'cask' | 'kafka';

