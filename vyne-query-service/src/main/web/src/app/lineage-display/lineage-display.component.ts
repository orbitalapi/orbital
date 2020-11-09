import {Component, Input, OnInit} from '@angular/core';
import {
  isOperationResult,
  OperationResultDataSource, RemoteCall,
} from '../services/query.service';

import {
  isTypeNamedInstance,
  SchemaGraphLink,
  SchemaGraphNode,
  SchemaNodeSet,
  TypedInstance,
  isTypedCollection,
  TypeNamedInstance,
  DataSource
} from '../services/schema';
import {BaseGraphComponent} from '../inheritence-graph/base-graph-component';
import {Subject} from 'rxjs';

type LineageElement = TypeNamedInstance | TypeNamedInstance[] | DataSource;

@Component({
  selector: 'app-lineage-display',
  templateUrl: './lineage-display.component.html',
  styleUrls: ['./lineage-display.component.scss']
})
export class LineageDisplayComponent extends BaseGraphComponent {
  static NODE_ID = '__nodeId';

  private _dataSource: DataSource;
  private _instance: TypeNamedInstance;

  graphNodesChanged = new Subject<boolean>()


  @Input()
  get dataSource(): DataSource {
    return this._dataSource;
  }

  set dataSource(value: DataSource) {
    if (this._dataSource === value) {
      return;
    }
    this._dataSource = value;
    this.schemaGraph = this.buildGraph(this.instance);
  }

  @Input()
  get instance(): TypeNamedInstance {
    return this._instance;
  }

  set instance(value: TypeNamedInstance) {
    if (this._instance === value) {
      return;
    }
    this._instance = value;
    this.schemaGraph = this.buildGraph(this.instance);
  }

  schemaGraph: SchemaNodeSet = this.emptyGraph();

  private buildGraph(node: LineageElement, linkTo: SchemaGraphNode = null): SchemaNodeSet {
    if (!node || !this.dataSource) {
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
      const instanceId = nodeId(instance, () => instance.typeName +  (Math.random() * 10000));
      return {
        id: instanceId,
        nodeId: instanceId,
        label: instance.value,
        subHeader: instance.typeName,
        value: instance,
        type: 'TYPE'
      } as SchemaGraphNode;
    }

    function collectionToNode(instance:  TypeNamedInstance[]): SchemaGraphNode {
      var typeName = instance[0] ? instance[0].typeName : 'asd'
      var value = instance.map( instance => instance.value)

      const instanceId = nodeId(instance, () => typeName + new Date().getTime());
      return {
        id: instanceId,
        nodeId: instanceId,
        label: value as any,
        subHeader: typeName,
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
      const instanceId = nodeId(dataSource, () => dataSource.dataSourceName + new Date().getTime());
      let label: string;
      switch (dataSource.dataSourceName) {
        case 'Provided':
          label = 'Provided as input';
          break;
        default:
          label = dataSource.dataSourceName;
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

    const buildDataSourceTo = (source: DataSource, typedInstanceNode: SchemaGraphNode) =>  {
      const dataSourceNodes = this.buildGraph(source, typedInstanceNode);
      this.appendNodeSet(dataSourceNodes, nodeSet);
    }

    if (isTypeNamedInstance(node)) {
      const typedInstanceNode = instanceToNode(node);
      nodes.push(typedInstanceNode);

      if (node.source) {
        buildDataSourceTo(node.source, typedInstanceNode)
      } else if(node === this.instance) { // Are we building the root level node?
        buildDataSourceTo(this.dataSource, typedInstanceNode)
      }
    }
    else if(isTypedCollection(node)) {
      const typedCollectionNode = collectionToNode(node);
      nodes.push(typedCollectionNode);

    // Take the datasource from the first node for now. THat's the best we can do
    // IN the future, enrich the API response to include datasource for TypedCOllections
      const source = node[0] ? node[0].source : null
      if (source) {
        buildDataSourceTo(source, typedCollectionNode)
      }

    }
    else if (isOperationResult(node)) {
      const remoteCallNode = remoteCallToNode(node.remoteCall, node);
      if (remoteCallNode.nodeId !== linkTo.nodeId) {
        nodes.push(remoteCallNode);
        links.push({
          source: remoteCallNode.nodeId,
          target: linkTo.nodeId,
          label: 'provided'
        });
        node.inputs.forEach(param => {
          var inputNode = undefined;
          if(Array.isArray(param.value)) {
            inputNode = collectionToNode(param.value);
          } else {
            inputNode = instanceToNode(param.value);
          }

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
