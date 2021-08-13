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

export interface MultiTargetPipelineSpec {
  name: string;
  input: PipelineTransportSpec;
  outputs: PipelineTransportSpec[];
}

export interface PipelineSpec {
  name: string;
  input: PipelineTransportSpec;
  output: PipelineTransportSpec;
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
