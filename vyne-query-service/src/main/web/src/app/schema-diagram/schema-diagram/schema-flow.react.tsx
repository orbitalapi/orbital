import * as React from 'react';
import ReactFlow, { Edge, Node, ReactFlowInstance } from 'react-flow-renderer';
import { ElementRef } from '@angular/core';
import * as ReactDOM from 'react-dom';
import ModelNode from './diagram-nodes/model-node';
import ApiNode from './diagram-nodes/api-service-node';
import KafkaNode from './diagram-nodes/kafka-service-node';
import { MemberWithLinks } from './schema-chart-builder';
import { SchemaChartController } from './schema-chart.controller';
import { QualifiedName } from '../../services/schema';

export type NodeType = 'Model' | 'Api' | 'Kafka';
type ReactComponentFunction = ({ data }: { data: any }) => JSX.Element
type NodeMap = { [key in NodeType]: ReactComponentFunction }

const nodeTypes: NodeMap = {
  'Model': ModelNode,
  'Api': ApiNode,
  'Kafka': KafkaNode
}

export class SchemaFlowWrapper {
  static initialize(
    elementRef: ElementRef,
    controller: SchemaChartController,
    width: number = 800,
    height: number = 600
  ) {

    function initHandler(instance: ReactFlowInstance) {
      controller.instance = instance;
    }


    ReactDOM.render(
      <div style={{ height: height, width: width }}>
        <ReactFlow
          onInit={initHandler}
          connectOnClick={false}
          nodes={controller.state.nodes}
          edges={controller.state.edges} nodeTypes={nodeTypes}
        />
      </div>,
      elementRef.nativeElement
    )
  }
}

export interface SchemaChartNodeSet {
  nodes: Node<MemberWithLinks>[];
  edges: Edge[]
}

export class SchemaChartState implements SchemaChartNodeSet {
  constructor(public readonly nodes: Node<MemberWithLinks>[], public readonly edges: Edge[]) {
  }

  findNodeForMember(name: QualifiedName): Node<MemberWithLinks> | null {
    return this.nodes.find(node => node.data.member.name.parameterizedName === name.parameterizedName)
  }


  /**
   * Adds the node into the state, if another node
   * with the same id is not already present.
   *
   * Returns a boolean indicating if the node was added.
   */
  addNodeIfNotPresent(node: Node<MemberWithLinks>): boolean {
    if (this.nodes.find(n => n.id === node.id)) {
      return false;
    } else {
      this.nodes.push(node);
      return true;
    }

  }
}
