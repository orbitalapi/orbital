import * as React from 'react';
import ReactFlow, { Edge, Node } from 'react-flow-renderer';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';
import ModelNode from './diagram-nodes/model-node';
import { SchemaMember } from '../../services/schema';
import ApiNode from './diagram-nodes/api-service-node';
import KafkaNode from './diagram-nodes/kafka-service-node';

export type NodeType = 'Model' | 'Api' | 'Kafka';
type ReactComponentFunction = ({ data }: { data: any }) => JSX.Element
type NodeMap = { [key in NodeType]: ReactComponentFunction }

const nodeTypes: NodeMap = {
  'Model': ModelNode,
  'Api': ApiNode,
  'Kafka' : KafkaNode
}

export class SchemaFlowWrapper {
  static initialize(
    elementRef: ElementRef,
    state: SchemaChartState,
    width: number = 800,
    height: number = 600
  ) {
    ReactDOM.render(
      <div style={{ height: height, width: width }}>
        <ReactFlow nodes={state.nodes} edges={state.edges} nodeTypes={nodeTypes}/>
      </div>,
      elementRef.nativeElement
    )
  }
}

export interface SchemaChartState {
  nodes: Node<SchemaMember>[]
  edges: Edge[]
}
