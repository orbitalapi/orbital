import {Component, Input, OnInit} from '@angular/core';
import {InstanceLike} from '../object-view/object-view.component';
import {
  DataSource,
  isOperationResult,
  isTypeNamedInstance,
  LineageGraph, OperationResultDataSource, RemoteCall,
  TypeNamedInstance
} from '../services/query.service';

import {SchemaGraphLink, SchemaGraphNode, SchemaNodeSet} from '../services/schema';
import {BaseGraphComponent} from '../inheritence-graph/base-graph-component';
import {Subject} from 'rxjs';

type LineageElement = TypeNamedInstance | DataSource;

@Component({
  selector: 'app-lineage-display',
  templateUrl: './lineage-display.component.html',
  styleUrls: ['./lineage-display.component.scss']
})
export class LineageDisplayComponent extends BaseGraphComponent {
  static NODE_ID = '__nodeId';

  private _lineageGraph: LineageGraph;
  private _instance: TypeNamedInstance;

  graphNodesChanged = new Subject<boolean>()


  @Input()
  get lineageGraph(): LineageGraph {
    return this._lineageGraph;
  }

  set lineageGraph(value: LineageGraph) {
    this._lineageGraph = value;
    this.schemaGraph = this.buildGraph(this.instance);
  }

  @Input()
  get instance(): TypeNamedInstance {
    return this._instance;
  }

  set instance(value: TypeNamedInstance) {
    this._instance = value;
    this.schemaGraph = this.buildGraph(this.instance);
  }

  schemaGraph: SchemaNodeSet = this.emptyGraph();

  private buildGraph(node: LineageElement, linkTo: SchemaGraphNode = null): SchemaNodeSet {
    if (!node || !this.lineageGraph) {
      return this.emptyGraph();
    }
    const self = this;

    function nodeId(instance: any, generator: () => string): string {
      if (!instance[LineageDisplayComponent.NODE_ID]) {
        instance[LineageDisplayComponent.NODE_ID] = self.makeSafeId(generator());
      }
      return instance[LineageDisplayComponent.NODE_ID];
    }


    function instanceToNode(instance: TypeNamedInstance): SchemaGraphNode {
      const instanceId = nodeId(instance, () => instance.typeName + new Date().getTime());
      return {
        id: instanceId,
        nodeId: instanceId,
        label: instance.value,
        subHeader: instance.typeName,
        value: instance,
        type: 'TYPE'
      } as SchemaGraphNode;
    }

    function remoteCallToNode(remoteCall: RemoteCall, dataSource: OperationResultDataSource): SchemaGraphNode {
      const instanceId = nodeId(remoteCall, () => remoteCall.operationQualifiedName + new Date().getTime());
      return {
        id: instanceId,
        nodeId: instanceId,
        label: remoteCall.operation,
        subHeader: 'Operation',
        value: dataSource,
        type: 'OPERATION'
      };
    }

    function dataSourceToNode(dataSource: DataSource): SchemaGraphNode {
      const instanceId = nodeId(dataSource, () => dataSource.name + new Date().getTime());
      let label: string;
      switch (dataSource.name) {
        case 'Provided':
          label = 'Provided as input';
          break;
        default:
          label = dataSource.name;
      }
      return {
        id: instanceId,
        nodeId: instanceId,
        subHeader: 'Fixed',
        label: label,
        value: dataSource,
        type: 'DATASOURCE'
      };
    }

    const nodes: SchemaGraphNode[] = [];
    const links: SchemaGraphLink[] = [];
    const nodeSet: SchemaNodeSet = {
      nodes,
      links
    };

    if (isTypeNamedInstance(node)) {
      const typedInstanceNode = instanceToNode(node);
      nodes.push(typedInstanceNode);

      if (node.source) {
        const dataSource = this.lineageGraph[node.source.dataSourceIndex];
        if (!dataSource) {
          throw new Error(`node declares data source with index ${node.source.dataSourceIndex} but no such index exists`);
        }
        const dataSourceNodes = this.buildGraph(dataSource, typedInstanceNode);
        this.appendNodeSet(dataSourceNodes, nodeSet);
      }
    } else if (isOperationResult(node)) {
      const remoteCallNode = remoteCallToNode(node.remoteCall, node);
      if (remoteCallNode.nodeId !== linkTo.nodeId) {
        nodes.push(remoteCallNode);
        links.push({
          source: remoteCallNode.nodeId,
          target: linkTo.nodeId,
          label: 'provided'
        });
        node.inputs.forEach(param => {
          const inputNode = instanceToNode(param.value);
          nodes.push(inputNode);
          links.push({
            source: inputNode.nodeId,
            target: remoteCallNode.nodeId,
            label: 'input'
          });
        });
      }
    } else {
      const dataSource = node as DataSource;
      const dataSourceNode = dataSourceToNode(dataSource);
      if (dataSourceNode.nodeId !== linkTo.nodeId) {
        nodes.push(dataSourceNode);
        links.push({
          source: dataSourceNode.nodeId,
          target: linkTo.nodeId,
          label: 'provided'
        });
      }
    }

    return nodeSet;
  }


  nodeSelected(selectedNode: SchemaGraphNode) {
    const newNodes = this.buildGraph(selectedNode.value as LineageElement, selectedNode);
    this.appendNodeSet(newNodes, this.schemaGraph);
    this.graphNodesChanged.next(true);
  }

  showServiceName(node): boolean {
    return node.type === 'OPERATION' || node.type === 'MEMBER';
  }

  serviceName(node: SchemaGraphNode): string {
    const dataSource = node.value as OperationResultDataSource;
    const remoteCall = dataSource.remoteCall;
    const parts = remoteCall.service.split('.')
    return parts[parts.length - 1];
  }

  operationName(node): string {
    const dataSource = node.value as OperationResultDataSource;
    const remoteCall = dataSource.remoteCall;
    return remoteCall.operation
  }

}
