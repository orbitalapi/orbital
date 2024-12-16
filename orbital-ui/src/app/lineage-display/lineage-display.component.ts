import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input} from '@angular/core';
import {
  EvaluatedExpressionDataSource, EvaluatedWhenCaseSelection,
  FailedEvaluatedExpressionDataSource,
  isEvaluatedExpressionDataSource,
  isFailedEvaluatedExpressionDataSource,
  isOperationResult, isWhenCaseDataSource,
  OperationResultReference,
  QueryService,
  RemoteCall,
} from '../services/query.service';

import {
  DataSource,
  isMappedSynonym,
  isTypedCollection,
  isTypeNamedInstance,
  isUntypedInstance, QualifiedName,
  ReferenceRepository,
  SchemaGraphLink,
  SchemaGraphNode,
  SchemaGraphNodeType,
  SchemaNodeSet, TypedInstance,
  TypeNamedInstance
} from '../services/schema';
import {BaseGraphComponent} from '../inheritence-graph/base-graph-component';
import {Subject} from 'rxjs';
import {isNullOrUndefined} from 'util';
import {QueryResultMemberCoordinates} from '../query-panel/instance-selected-event';
import {QualifiedNameParser} from "../services/qualified-name-parser";
import {isObjectWithProperty} from "../utils/utils";

type LineageElement = TypeNamedInstance | TypeNamedInstance[] | DataSource;

@Component({
  selector: 'app-lineage-display',
  templateUrl: './lineage-display.component.html',
  styleUrls: ['./lineage-display.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LineageDisplayComponent extends BaseGraphComponent {
  static NODE_ID = '__nodeId';

  private _dataSource: DataSource;
  private _instance: TypeNamedInstance;

  constructor(private queryHistoryService: QueryService, private changeDetector: ChangeDetectorRef) {
    super();
  }

  fullscreen = false;

  toggleFullscreen() {
    this.fullscreen = !this.fullscreen;
  }

  @Input()
  instanceQueryCoordinates: QueryResultMemberCoordinates;

  graphNodesChanged = new Subject<boolean>()

  private remoteCallRepository = new ReferenceRepository<RemoteCall>()

  private loadedDataSources: { [key: string]: DataSource };

  @Input()
  get dataSource(): DataSource {
    return this._dataSource;
  }

  set dataSource(value: DataSource) {
    if (this._dataSource === value) {
      return;
    }
    this._dataSource = value;
    this.loadedDataSources = {};
    this.appendLoadedDataSource(value);
    this.schemaGraph = this.buildFullGraph(this.instance);
  }

  private appendLoadedDataSource(dataSource: DataSource) {
    this.loadedDataSources[dataSource.id] = dataSource;
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

  serviceName(node: SchemaGraphNode): string {
    const dataSource = node.value as OperationResultReference;
    return dataSource.serviceDisplayName;
  }

  /**
   * Adds the provided node to the set of existing nodes if not already present.
   * Returns the node in the collection (either the newly added node, or the existing
   * one that was already present)
   */
  private addNodeIfNotPresent(nodes:SchemaGraphNode[], newNode: SchemaGraphNode):SchemaGraphNode {
    const existing = nodes.find(existing => existing.id == newNode.id )
    if (existing) {
      return existing
    } else {
      nodes.push(newNode)
      return newNode
    }
  }

  private buildExpressionNode(node: EvaluatedExpressionDataSource, dataSourceToNode: (dataSource: DataSource) => SchemaGraphNode, nodes: SchemaGraphNode[], links: SchemaGraphLink[], linkTo: SchemaGraphNode, instanceToNode: (instance: TypeNamedInstance) => SchemaGraphNode, nodeSet: SchemaNodeSet) {
    const expressionDataSource = node as EvaluatedExpressionDataSource;
    const dataSourceNode = dataSourceToNode(expressionDataSource);
    this.appendLoadedDataSource(node);
    this.addNodeIfNotPresent(nodes, dataSourceNode)
    links.push({
      source: dataSourceNode.nodeId,
      target: linkTo.nodeId,
      label: 'returned'
    })



    expressionDataSource.inputs
      // Don't add nodes for constants - which turn up as 'Undefined source' -- it makes the diagram ugly,
      // and we don't need a node that shows the creation of a constant - the constant value is shown in the label
      .filter(inputNode => inputNode.source.id !== 'Undefined source')
      .forEach(param => {
        const inputNode = instanceToNode(param);
        const addedInputNode = this.addNodeIfNotPresent(nodes, inputNode)
        links.push({
          source: addedInputNode.nodeId,
          target: dataSourceNode.nodeId,
          label: 'input'
        });
        const inputNodes = this.buildGraph(param, addedInputNode, []);
        this.appendNodeSet(inputNodes, nodeSet);
    })
  }

  private buildWhenClauseNode(node: EvaluatedWhenCaseSelection, dataSourceToNode: (dataSource: DataSource) => SchemaGraphNode, nodes: SchemaGraphNode[], links: SchemaGraphLink[], linkTo: SchemaGraphNode, instanceToNode: (instance: TypeNamedInstance) => SchemaGraphNode, nodeSet: SchemaNodeSet) {
    const expressionDataSource = node as EvaluatedWhenCaseSelection;
    const dataSourceNode = dataSourceToNode(expressionDataSource);
    const thisWhenClauseNodes: SchemaGraphNode[] = [];
    const thisWhenClauseLinks: SchemaGraphLink[] = [];
    const thisNodeSet: SchemaNodeSet = {
      nodes: thisWhenClauseNodes,
      links: thisWhenClauseLinks
    }
    this.appendLoadedDataSource(node);
    const addedWhenNode = this.addNodeIfNotPresent(thisWhenClauseNodes, dataSourceNode)
    links.push({
      source: addedWhenNode.nodeId,
      target: linkTo.nodeId,
      label: 'returned value'
    })

    expressionDataSource.evaluatedCases.forEach(evaluatedCase => {
      const inputNode = instanceToNode(evaluatedCase);
      const addedCaseNode = this.addNodeIfNotPresent(thisWhenClauseNodes, inputNode)
      links.push({
        source: addedCaseNode.nodeId,
        target: dataSourceNode.nodeId,
        label: 'input'
      });
      const inputNodes = this.buildGraph(evaluatedCase, inputNode, [], thisNodeSet);
      this.appendNodeSet(inputNodes, thisNodeSet);
    })
    this.appendNodeSet(thisNodeSet, nodeSet)
  }

  nodeSelected(selectedNode: SchemaGraphNode) {
    if (!isNullOrUndefined(selectedNode.value.dataSourceId)) {
      const dataSourceId = selectedNode.value.dataSourceId;
      if (!this.loadedDataSources[dataSourceId]) {
        this.loadAdditionalDataSource(dataSourceId, selectedNode);
      }
    }
  }

  loadAdditionalDataSource(dataSourceId: string, linkTo: SchemaGraphNode) {
    if (!this.instanceQueryCoordinates) {
      return;
    }
    this.queryHistoryService.getLineageRecord(dataSourceId).subscribe(result => {
      const source = result.dataSource;
      this.appendLoadedDataSource(source);
      const newNodes = this.buildGraph(source, linkTo)
      this.appendNodeSet(newNodes, this.schemaGraph);
      this.graphNodesChanged.next(true);
      this.changeDetector.markForCheck();
    })
  }

  showServiceName(node): boolean {
    return node.type === 'OPERATION' || node.type === 'MEMBER';
  }

  operationName(node): string {
    const dataSource = node.value as OperationResultReference;
    return dataSource.operationDisplayName
  }

  nodeId(instance: any, generator: () => string): string {
    if (!instance[LineageDisplayComponent.NODE_ID]) {
      // Attempting to prevent the same object being added to the chart multiple times.
      // If there's a server-side id here, use it.
      if (isObjectWithProperty(instance, "nodeId")) {
        instance[LineageDisplayComponent.NODE_ID] = instance.nodeId;
      } else {
        instance[LineageDisplayComponent.NODE_ID] = this.makeSafeId(generator());
      }
    }
    return instance[LineageDisplayComponent.NODE_ID];
  }

  instanceToNode(instance: TypeNamedInstance, displayedValue: any | null = null): SchemaGraphNode {
    if (instance.source) {
      this.appendLoadedDataSource(instance.source);
    }
    const instanceId = this.nodeId(instance, () => {
      return (Array.isArray(instance)) ? 'Array' + (Math.random() * 10000) : instance.typeName + (Math.random() * 10000)
    });
    let label = '';
    if (Array.isArray(instance)) {
      label = 'Multiple values';
    } else if (Array.isArray(instance.value)) {
      // We'll get here if the value was a TypedCollction
      const values = instance.value as TypeNamedInstance[]
      // TODO : We should be truncating this if there's hundreds
      label = JSON.stringify(values.map(v => v.value))
    } else {
      label = isNullOrUndefined(instance.value) ? 'Null value' : displayedValue || instance.value;
    }
    const qualifiedName = QualifiedNameParser.parse(instance.typeName)
    const shortDisplayName = qualifiedName.shortDisplayName
    return {
      id: instanceId,
      nodeId: instanceId,
      label: label,
      subHeader: shortDisplayName,
      value: instance,
      type: 'TYPE'
    } as SchemaGraphNode;
  };

  private buildGraph(node: LineageElement, linkTo: SchemaGraphNode = null, nodesUnderConstruction: LineageElement[] = [], appendTo: SchemaNodeSet = {nodes: [], links: []}): SchemaNodeSet {
    if (!node || !this.dataSource) {
      return this.emptyGraph();
    }
    if (isUntypedInstance(node)) {
      // If we get given an untypedInstance, we should wait, as there's a real instance coming
      // after the server call completes
      return this.emptyGraph();
    }
    const self = this;

    const collectionToNode = (instance: TypeNamedInstance[]): SchemaGraphNode => {
      var typeName = instance[0] ? instance[0].typeName : 'asd'
      var value = instance.map(instance => instance.value)

      const instanceId = this.nodeId(instance, () => typeName + new Date().getTime());
      return {
        id: instanceId,
        nodeId: instanceId,
        label: value as any,
        subHeader: typeName,
        value: instance,
        type: 'TYPE'
      } as SchemaGraphNode;
    };

    function remoteCallToNode(dataSource: OperationResultReference): SchemaGraphNode {
      self.appendLoadedDataSource(dataSource);
      const instanceId = self.nodeId(dataSource, () => dataSource.operationName.fullyQualifiedName + new Date().getTime());
      return {
        id: instanceId,
        nodeId: instanceId,
        label: dataSource.operationDisplayName,
        subHeader: 'Operation',
        value: dataSource,
        type: 'OPERATION'
      };
    }

    function dataSourceToNode(dataSource: DataSource): SchemaGraphNode {
      self.appendLoadedDataSource(dataSource);
      const instanceId = self.nodeId(dataSource, () => dataSource.id || (dataSource.dataSourceName + new Date().getTime()));
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
        case 'Select case':
          const evaluatedWhenCase = dataSource as EvaluatedWhenCaseSelection;
          subHeader = 'Evaluated when block'
          label = evaluatedWhenCase.matchedExpressionTaxi || 'No case matched';
          type = 'WHEN_BLOCK_RESULT';

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

    const nodes: SchemaGraphNode[] = appendTo.nodes;
    const links: SchemaGraphLink[] = appendTo.links;
    const nodeSet: SchemaNodeSet = appendTo;

    const buildDataSourceTo = (source: DataSource, typedInstanceNode: SchemaGraphNode) => {
      const dataSourceNodes = this.buildGraph(source, typedInstanceNode);
      this.appendLoadedDataSource(source);
      this.appendNodeSet(dataSourceNodes, nodeSet);
    }

    nodesUnderConstruction.push(node)

    if (isTypeNamedInstance(node)) {
      const typedInstanceNode = this.instanceToNode(node);
      this.addNodeIfNotPresent(nodes, typedInstanceNode);

      if (node.source) {

        buildDataSourceTo(node.source, typedInstanceNode)
      } else if (node === this.instance) { // Are we building the root level node?
        buildDataSourceTo(this.dataSource, typedInstanceNode)
      }
    } else if (isTypedCollection(node)) {
      const typedCollectionNode = collectionToNode(node);
      this.addNodeIfNotPresent(nodes,typedCollectionNode)

      // Take the datasource from the first node for now. THat's the best we can do
      // IN the future, enrich the API response to include datasource for TypedCOllections
      const source = node[0] ? node[0].source : null
      if (source) {
        buildDataSourceTo(source, typedCollectionNode)
      }

    } else if (isOperationResult(node)) {
      const remoteCallNode = remoteCallToNode(node);
      if (remoteCallNode.nodeId !== linkTo.nodeId) {
        this.addNodeIfNotPresent(nodes, remoteCallNode)
        links.push({
          source: remoteCallNode.nodeId,
          target: linkTo.nodeId,
          label: 'provided'
        });
        node.inputs.forEach(param => {
          let inputNode = undefined;
          if (Array.isArray(param.value)) {
            inputNode = collectionToNode(param.value);
          } else if (this.isRequestObject(param.value)) {
            inputNode = this.appendRequestObject(nodes, links, param.value, remoteCallNode.nodeId)
          } else {
            inputNode = this.instanceToNode(param.value);
          }
          this.addNodeIfNotPresent(nodes, inputNode)
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
      const inputNode = this.instanceToNode(synonymSource);
      this.addNodeIfNotPresent(nodes, inputNode)
      links.push({
        source: inputNode.nodeId,
        target: linkTo.nodeId,
        label: 'Is synonym of'
      })
    } else if (isEvaluatedExpressionDataSource(node) || isFailedEvaluatedExpressionDataSource(node)) {
      this.buildExpressionNode(node, dataSourceToNode, nodes, links, linkTo, this.instanceToNode.bind(this), nodeSet);
    } else if (isWhenCaseDataSource(node)) {
      this.buildWhenClauseNode(node, dataSourceToNode, nodes, links, linkTo, this.instanceToNode.bind(this), nodeSet);
    } else {
      const dataSource = node as DataSource;
      this.appendLoadedDataSource(dataSource);
      const dataSourceNode = dataSourceToNode(dataSource);
      if (dataSourceNode.nodeId !== linkTo.nodeId) {
        this.addNodeIfNotPresent(nodes, dataSourceNode)
        links.push({
          source: dataSourceNode.nodeId,
          target: linkTo.nodeId,
          label: 'provided'
        });
      }
    }

    return nodeSet;
  }

  /**
   * Determines if the typed instance is a request object, rather than a scalar value
   */
  private isRequestObject(instance: TypeNamedInstance) {
    return typeof instance.value === "object"
  }

  private appendRequestObject(nodes: SchemaGraphNode[], links: SchemaGraphLink[], requestObject: TypeNamedInstance, targetNodeId: string): SchemaGraphNode {
    const requestObjectNode = this.instanceToNode(requestObject, "Request Payload")
    this.addNodeIfNotPresent(nodes, requestObjectNode);

    Object.keys(requestObject.value).forEach(fieldName => {
      const fieldValue = requestObject.value[fieldName] as TypeNamedInstance;
      const fieldValueNode = this.instanceToNode(fieldValue);
      this.addNodeIfNotPresent(nodes, fieldValueNode);
      links.push({
        source: fieldValueNode.nodeId,
        target: requestObjectNode.nodeId,
        label: fieldName
      });
    })
    return requestObjectNode;
  }
}

