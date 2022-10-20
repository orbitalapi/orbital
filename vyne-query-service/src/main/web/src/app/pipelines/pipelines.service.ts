import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { VyneServicesModule } from '../services/vyne-services.module';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs/internal/Observable';
import { map } from 'rxjs/operators';

// TODO Make the UI support multiple pipeline outputs

function addOutput(pipelineSummary: RunningPipelineSummary): RunningPipelineSummary {
  pipelineSummary.pipeline.spec.output = (pipelineSummary.pipeline.spec as any).outputs[0];
  return pipelineSummary;
}

function addOutputs(pipelineSpec: PipelineSpec): PipelineSpec {
  (pipelineSpec as any).outputs = [pipelineSpec.output];
  delete pipelineSpec.output;
  return pipelineSpec;
}

@Injectable({
  providedIn: VyneServicesModule
})
export class PipelineService {
  constructor(private http: HttpClient) {
  }

  listPipelines(): Observable<RunningPipelineSummary[]> {
    return this.http.get<RunningPipelineSummary[]>(`${environment.serverUrl}/api/pipelines`).pipe(map(pipelines => pipelines.map(pipelineSummary => addOutput(pipelineSummary))));
  }

  submitPipeline(pipelineSpec: PipelineSpec): Observable<PipelineStateSnapshot> {
    return this.http.post<PipelineStateSnapshot>(`${environment.serverUrl}/api/pipelines`, addOutputs(pipelineSpec));
  }

  deletePipeline(pipelineId: string): Observable<PipelineStatus> {
    return this.http.delete<PipelineStatus>(`${environment.serverUrl}/api/pipelines/${pipelineId}`);
  }

  getPipeline(pipelineId: string): Observable<RunningPipelineSummary> {
    return this.http.get<RunningPipelineSummary>(`${environment.serverUrl}/api/pipelines/${pipelineId}`).pipe(map(pipelineSummary => addOutput(pipelineSummary)));
  }
}

export interface RunningPipelineSummary {
  pipeline: SubmittedPipeline | null;
  status: PipelineStatus;
}

export interface SubmittedPipeline {
  name: string;
  jobId: string;
  spec: PipelineSpec;
  dotViz: string;
  graph: DagDataset;
  pipelineSpecId: string;
}

export interface PipelineStatus {
  name: string;
  id: string;
  status: PipelineJobStatus;
  submissionTime: Date;
  metrics: PipelineMetrics;
}

export interface PipelineMetrics {
  receivedCount: MetricValueSet[];
  emittedCount: MetricValueSet[];
  inflight: MetricValueSet[];
  queueSize: MetricValueSet[];
}

export interface MetricValueSet {
  address: string;
  measurements: MetricValue[];
  latestValue: MetricValue;
}

export interface MetricValue {
  value: any;
  timestamp: Date | String;
}

export type PipelineJobStatus =
/**
 * The job is submitted but hasn't started yet. A job also enters this
 * state when its execution was interrupted (e.g., due to a cluster member
 * failing), before it is started again.
 */
  'NOT_RUNNING' |

  /**
   * The job is in the initialization phase on a new coordinator.
   */
  'STARTING' |

  /**
   * The job is currently running.
   */
  'RUNNING' |

  /**
   * The job is suspended and it can be manually resumed.
   */
  'SUSPENDED' |

  /**
   * The job is suspended and is exporting the snapshot. It cannot be resumed
   * until the export is finished and status is [.SUSPENDED] again.
   */
  'SUSPENDED_EXPORTING_SNAPSHOT' |

  /**
   * The job is currently being completed.
   */
  'COMPLETING' |

  /**
   * The job has failed with an exception.
   */
  'FAILED' |

  /**
   * The job has completed successfully.
   */
  'COMPLETED';

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
    label: 'HTTP trigger',
    description: 'Expose an HTTP endpoint that when hit, will trigger this pipeline with data',
    icon: ''
  },
  {
    type: 'taxiOperation',
    label: 'HTTP Poll',
    description: 'Poll a defined HTTP endpoint for data',
    icon: ''
  },
  {
    type: 'query',
    label: 'Vyne query',
    description: 'Execute a Vyne query on a given interval',
    icon: ''
  },
  {
    type: 'kafka',
    label: 'Kafka topic',
    description: 'Subscribe to a kafka topic for messages',
    icon: ''
  },
  {
    type: 'awsSqsS3',
    label: 'AWS S3 SQS source',
    description: 'Poll SQS queue for S3 object creation notifications',
    icon: ''
  }
];

export interface PipelineSpec {
  name: string;
  input: PipelineTransportSpec;
  output: PipelineTransportSpec;

  // Only valid when sent from the server
  id?: string;
  // Only valid when sent from the server
  description?: string;
}

export interface PipelineTransportSpec {
  type: PipelineTransportType;
  direction: PipelineDirection;
  // Pipeline specs support arbitary properties, defined
  // on their individual contracts serverside
  [x: string]: any;
}

export const PIPELINE_OUTPUTS: PipelineTransport[] = [
  {
    type: 'kafka',
    label: 'Kafka topic',
    description: 'Publish a message to a Kafka topic',
    icon: ''
  },
  {
    type: 'taxiOperation',
    label: 'Call an operation',
    description: 'Call an HTTP operation with data',
    icon: ''
  },
  {
    type: 'jdbc',
    label: 'JDBC store',
    description: 'Publish a message to a JDBC store',
    icon: ''
  },
  {
    type: 'awsS3',
    label: 'AWS S3',
    description: 'Publish a message to AWS S3 as a CSV file',
    icon: ''
  }
];


export type PipelineDirection = 'INPUT' | 'OUTPUT';
export type PipelineTransportType =
  'httpListener'
  | 'taxiOperation'
  | 'cask'
  | 'kafka'
  | 'awsSqsS3'
  | 'awsS3'
  | 'jdbc'
  | 'query';

export interface DagDataset {
  nodes: DagGraphNode[];
  links: DagGraphLink[];
}

export interface DagGraphNode {
  id: string;
  label: string;
}

export interface DagGraphLink {
  source: string;
  target: string;
  label: string;
}
