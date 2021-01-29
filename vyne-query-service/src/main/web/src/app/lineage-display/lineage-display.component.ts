import {Component, Input} from '@angular/core';
import {
  EvaluatedExpressionDataSource, FailedEvaluatedExpressionDataSource,
  isEvaluatedExpressionDataSource, isFailedEvaluatedExpressionDataSource,
  isOperationResult,
  OperationResultDataSource,
  RemoteCall,
} from '../services/query.service';

import {
  DataSource,
  isMappedSynonym,
  isTypedCollection,
  isTypeNamedInstance,
  isUntypedInstance, ReferenceOrInstance, ReferenceRepository,
  SchemaGraphLink,
  SchemaGraphNode, SchemaGraphNodeType,
  SchemaNodeSet,
  TypeNamedInstance
} from '../services/schema';
import {BaseGraphComponent} from '../inheritence-graph/base-graph-component';
import {Subject} from 'rxjs';
import {isNullOrUndefined, isString} from 'util';

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

  private remoteCallRepository = new ReferenceRepository<RemoteCall>()

  @Input()
  get dataSource(): DataSource {
    return this._dataSource;
  }

  set dataSource(value: DataSource) {
    if (this._dataSource === value) {
      return;
    }
    this._dataSource = value;
    this.schemaGraph = this.buildFullGraph(this.instance);
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
    this.schemaGraph = this.buildFullGraph(this.instance);
  }

  schemaGraph: SchemaNodeSet = this.emptyGraph();

  private buildFullGraph(startNode: LineageElement): SchemaNodeSet {
    return this.buildGraph(startNode)
  }

  private buildGraph(node: LineageElement, linkTo: SchemaGraphNode = null, nodesUnderConstruction: LineageElement[] = []): SchemaNodeSet {
    if (!node || !this.dataSource) {
      return this.emptyGraph();
    }
    if (isUntypedInstance(node)) {
      // If we get given an untypedInstance, we should wait, as there's a real instance coming
      // after the server call completes
      return this.emptyGraph();
    }
    const self = this;
    if (nodesUnderConstruction.includes(node)) {
      return this.emptyGraph()
    }

    function nodeId(instance: any, generator: () => string): string {
      if (!instance[LineageDisplayComponent.NODE_ID]) {
        instance[LineageDisplayComponent.NODE_ID] = self.makeSafeId(generator());
      }
      return instance[LineageDisplayComponent.NODE_ID];
    }


    function instanceToNode(instance: TypeNamedInstance): SchemaGraphNode {
      const instanceId = nodeId(instance, () => instance.typeName + (Math.random() * 10000));
      const label = isNullOrUndefined(instance.value) ? 'Null value' : instance.value;
      return {
        id: instanceId,
        nodeId: instanceId,
        label: label,
        subHeader: instance.typeName,
        value: instance,
        type: 'TYPE'
      } as SchemaGraphNode;
    }

    function collectionToNode(instance: TypeNamedInstance[]): SchemaGraphNode {
      var typeName = instance[0] ? instance[0].typeName : 'asd'
      var value = instance.map(instance => instance.value)

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
      let subHeader = 'Fixed';
      let type: SchemaGraphNodeType = 'DATASOURCE';
      switch (dataSource.dataSourceName) {
        case 'Provided':
          label = 'Provided as input';
          break;
        case 'Evaluated expression':
          const evaluatedExpression = dataSource as EvaluatedExpressionDataSource;
          subHeader = 'Evaluated expression'
          label = evaluatedExpression.expressionTaxi;
          break;
        case 'Failed evaluated expression':
          const failedEvaluatedExpression = dataSource as FailedEvaluatedExpressionDataSource;
          subHeader = `Error in expression: ${failedEvaluatedExpression.errorMessage}`
          label = failedEvaluatedExpression.expressionTaxi;
          type = 'ERROR'
          break;
        case 'Mapped':
          label = dataSource.dataSourceName;
          if (isMappedSynonym(dataSource)) {
            subHeader = 'From Foo.Baz'
          }

        default:
          label = dataSource.dataSourceName;
      }
      return {
        id: instanceId,
        nodeId: instanceId,
        subHeader: subHeader,
        label: label,
        value: dataSource,
        type: type
      };
    }

    const nodes: SchemaGraphNode[] = [];
    const links: SchemaGraphLink[] = [];
    const nodeSet: SchemaNodeSet = {
      nodes,
      links
    };

    const buildDataSourceTo = (source: DataSource, typedInstanceNode: SchemaGraphNode) => {
      const dataSourceNodes = this.buildGraph(source, typedInstanceNode);
      this.appendNodeSet(dataSourceNodes, nodeSet);
    }

    nodesUnderConstruction.push(node)

    if (isTypeNamedInstance(node)) {
      const typedInstanceNode = instanceToNode(node);
      nodes.push(typedInstanceNode);

      if (node.source) {
        buildDataSourceTo(node.source, typedInstanceNode)
      } else if (node === this.instance) { // Are we building the root level node?
        buildDataSourceTo(this.dataSource, typedInstanceNode)
      }
    } else if (isTypedCollection(node)) {
      const typedCollectionNode = collectionToNode(node);
      nodes.push(typedCollectionNode);

      // Take the datasource from the first node for now. THat's the best we can do
      // IN the future, enrich the API response to include datasource for TypedCOllections
      const source = node[0] ? node[0].source : null
      if (source) {
        buildDataSourceTo(source, typedCollectionNode)
      }

    } else if (isOperationResult(node)) {
      const remoteCall = this.remoteCallRepository.getInstance(node.remoteCall);
      const remoteCallNode = remoteCallToNode(remoteCall, node);
      if (remoteCallNode.nodeId !== linkTo.nodeId) {
        nodes.push(remoteCallNode);
        links.push({
          source: remoteCallNode.nodeId,
          target: linkTo.nodeId,
          label: 'provided'
        });
        node.inputs.forEach(param => {
          let inputNode = undefined;
          if (Array.isArray(param.value)) {
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
          const inputNodes = this.buildGraph(param.value, inputNode, [])
          this.appendNodeSet(inputNodes, nodeSet)

        });
      }
    } else if (isMappedSynonym(node)) {
      const synonymSource = node.source
      const inputNode = instanceToNode(synonymSource);
      nodes.push(inputNode)
      links.push({
        source: inputNode.nodeId,
        target: linkTo.nodeId,
        label: 'Is synonym of'
      })
    } else if (isEvaluatedExpressionDataSource(node) || isFailedEvaluatedExpressionDataSource(node)) {
      const expressionDataSource = node as EvaluatedExpressionDataSource;
      const dataSourceNode = dataSourceToNode(expressionDataSource);
      nodes.push(dataSourceNode)
      links.push({
        source: dataSourceNode.nodeId,
        target: linkTo.nodeId,
        label: 'returned'
      })
      expressionDataSource.inputs.forEach(param => {
        const inputNode = instanceToNode(param);
        nodes.push(inputNode);
        links.push({
          source: inputNode.nodeId,
          target: dataSourceNode.nodeId,
          label: 'input'
        });
        const inputNodes = this.buildGraph(param, inputNode, []);
        this.appendNodeSet(inputNodes, nodeSet);
      })
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
    const remoteCall = this.remoteCallRepository.getInstance(dataSource.remoteCall);
    const parts = remoteCall.service.split('.')
    return parts[parts.length - 1];
  }

  operationName(node): string {
    const dataSource = node.value as OperationResultDataSource;
    const remoteCall = this.remoteCallRepository.getInstance(dataSource.remoteCall);
    return remoteCall.operation
  }

}

