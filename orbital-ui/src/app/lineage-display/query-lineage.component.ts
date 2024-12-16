import {AfterViewInit, Component, DestroyRef, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {BaseGraphComponent} from '../inheritence-graph/base-graph-component';
import {CacheNode, QuerySankeyChartRow, SankeyNodeType, SankeyOperationNodeDetails} from '../services/query.service';
import {ResizeObservableService} from '../services/resize-observable.service';
import {SchemaGraph, SchemaGraphLink, SchemaGraphNode, SchemaGraphNodeType, SchemaNodeSet} from '../services/schema';
import {ClusterNode, NgxGraphZoomOptions} from '@swimlane/ngx-graph';
import {Subject} from 'rxjs';
import {isNullOrUndefined} from "../utils/utils";
import {capitalizeFirstLetter} from "../utils/strings";

@Component({
  selector: 'app-query-lineage',
  templateUrl: './query-lineage.component.html',
  styleUrls: ['./query-lineage.component.scss'],
  providers: [ResizeObservableService]
})
export class QueryLineageComponent extends BaseGraphComponent implements AfterViewInit {

  /**
   * We provide an external setter / getter for fullscreen
   * This has become a neccessity because of a CSS issue in how we're displaying fullscreen.
   * When fullscreen is applied, we use display: fixed.
   * However, in very specific situations (if a parent to this component) is using css transform,
   * then display: fixed becomes relative to the parent, not to the window.
   *
   * https://stackoverflow.com/questions/21091958/css-fixed-child-element-positions-relative-to-parent-element-not-to-the-viewpo
   *
   * there's no real fix / workaround for this.
   * Removing the css transform isn't possible (ie., if it's in a 3rd party component such as Taiga).
   *
   * The way we've gotten around this is by duplicating the component.
   * Once in the place to display this, and then again outside of whatever is applying the
   * css transform (such as a Taiga accordion).
   *
   * We then ng-if to hide whichever of the two shouldn't be visible
   *
   */
  private _fullscreen = false;
  @Input()
  get fullscreen():boolean {
    return this._fullscreen;
  }
  set fullscreen(value) {
    if (this._fullscreen === value) return;
    this._fullscreen = value;
    this.fullscreenChange.emit(value)
    setTimeout(() => {
      this.redrawChart$.next('xx');
    });
  }

  @Output()
  fullscreenChange = new EventEmitter<boolean>();

  redrawChart$ = new Subject();

  private _rows: QuerySankeyChartRow[];
  schemaNodeSet: SchemaNodeSet;

  clusters: ClusterNode[] = [];

  zoomToFit$: Subject<NgxGraphZoomOptions> = new Subject();

  @ViewChild('chartOuterContianer')
  chartContainer: ElementRef;

  constructor(
    private resizeObservableService: ResizeObservableService,
    private destroyRef: DestroyRef
  ) {
    super();
  }

  ngAfterViewInit(): void {
    this.resizeObservableService.resizeObservable(this.chartContainer.nativeElement)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(resizeEntry => {
        requestAnimationFrame(() => {
          this.zoomToFit$.next({autoCenter: true, force: true});
        })
      })
  }

  filteredNode: SchemaGraphNode | null;

  @Input()
  get rows(): QuerySankeyChartRow[] {
    return this._rows;
  }

  set rows(value: QuerySankeyChartRow[]) {
    if (this.rows === value) {
      return;
    }
    this._rows = value;
    if (this.rows) {
      this.refreshChartData();
      setTimeout(() => {
        this.zoomToFit$.next({autoCenter: true, force: true});
      }, 100);
    }
  }

  private refreshChartData() {
    let rowData: QuerySankeyChartRow[];
    if (isNullOrUndefined(this.filteredNode)) {
      rowData = this.rows;
    } else {
      rowData = this.filterRowSet(this.rows, new Set([this.filteredNode.id]));
    }
    const [schemaNodeSet, clusterNodes] = this.generateChartData(rowData);
    this.schemaNodeSet = schemaNodeSet;
    this.clusters = clusterNodes;
  }

  private filterRowSet(rows: QuerySankeyChartRow[], filterToIds: Set<string>): QuerySankeyChartRow[] {
    const discoveredIds = new Set<string>(filterToIds);
    const filteredRows = rows.filter(r => {
      const sourceId = this.toSafeId(r.sourceNode);
      const targetId = this.toSafeId(r.targetNode);
      if (filterToIds.has(targetId)) {
        // We only want to filter things that link here, not where this thing lings
        discoveredIds.add(sourceId);
        // discoveredIds.add(targetId);
        return true;
      } else {
        return false;
      }
    });
    if (discoveredIds.size === filterToIds.size) {
      return filteredRows;
    } else {
      // We discovered new ids, so expand the search to include them
      return this.filterRowSet(rows, discoveredIds);
    }
  }


  private toSafeId(input: string): string {
    const result = input
      .split(' ').join('')
      .split('.').join('')
      .split('"').join('')
      .split('(').join('')
      .split(')').join('')
      .split('_').join('')
      .split(',').join('')
      .split('@@').join('');
    if (result === '') {
      return 'EMPTY_STRING';
    } else {
      // The graphing library throws exceptions if the
      // id of a node is the same as an attribute on the collection.
      // (eg., you can't have a node with the id of 'length')
      // So, append a safe character to the end.
      // Need to be careful here, as lots of characters aren't safe.
      return result + '$';
    }
  }

  nodeIcon(node: SchemaGraphNode) {
    if (node.type !== 'OPERATION') {
      return node.type
    } else {
      const nodeDetails = this.nodeDetails(node.data)
      return nodeDetails?.systemProductName || nodeDetails?.operationType;
    }

  }

  onNodeClicked(clickedNode: SchemaGraphNode) {
    if (clickedNode.type !== 'MEMBER') {
      // We don't currently do drill-down for things other than members (attributes)
      return;
    }
    this.filteredNode = clickedNode;
    this.refreshChartData();
  }

  clearFilter() {
    this.filteredNode = null;
    this.refreshChartData();
  }

  toggleFullscreen() {
    this.fullscreen = !this.fullscreen;
  }



  getHtmlVerbTextColor(nodeData: QueryLineageOperationalNode) {
    const defaultColor = '#7592a2';
    const verb = nodeData.nodeOperationData['verb'];
    if (!verb) {
      return defaultColor;
    }
    switch (verb) {
      // Selected some values at random from the Tailwind Css colors, using the 500 variants
      // https://tailwindcss.com/docs/customizing-colors
      case 'GET':
        return '#0ea5e9';
      case 'POST':
        return '#f59e0b';
      case 'PUT':
        return '#6366f1';
      case 'DELETE':
        return '#ef4444';
      default:
        return defaultColor;
    }
  }

  private nodeDetails(data: QueryLineageOperationalNode | null): SankeyOperationNodeDetails | null {
    if (!data) return null;
    return data.nodeOperationData;
  }

  private generateChartData(rows: QuerySankeyChartRow[]): [SchemaNodeSet, ClusterNode[]] {
    const nodes: Map<string, SchemaGraphNode> = new Map<string, SchemaGraphNode>();
    const links: Map<number, SchemaGraphLink> = new Map<number, SchemaGraphLink>();
    const attributeNodeIds = new Map<string, string>();


    function rowTypeToGraphType(rowType: SankeyNodeType): [SchemaGraphNodeType, string] {
      switch (rowType) {
        case 'AttributeName':
          return ['MEMBER', 'PROPERTY'];
        case 'Expression':
          return ['TYPE', 'FORMULA'];
        case 'ExpressionInput':
          return ['TYPE', 'FORMULA INPUT'];
        case 'ProvidedInput':
          return ['DATASOURCE', 'PROVIDED VALUE'];
        case 'QualifiedName':
          return ['OPERATION', 'SYSTEM'];
        case 'RequestObject':
          return ['REQUEST_OBJECT', 'Request'];
      }
    }

    function getNodeLabel(nodeText: string, nodeType: SankeyNodeType, operationData: SankeyOperationNodeDetails | null): [string, (string | null)] {
      if (operationData != null) {
        switch (operationData.operationType) {
          case 'Database':
            const dbHeader = 'Db Query: ' + operationData.connectionName;
            const dbSubHeader = 'Tables: ' + operationData.tableNames.join(', ');
            return [dbHeader, dbSubHeader]
          case 'KafkaTopic':
            const kafkaHeader = 'Kafka connection: ' + operationData.connectionName
            const kafkaSubHeader = 'Topic: ' + operationData.topic;
            return [kafkaHeader, kafkaSubHeader]
          case 'Http':
            const httpHeader = 'HTTP ' + operationData.verb
            const httpSubheader = operationData.operationName.name.replace('@@', ' / ');
            return [httpHeader, httpSubheader]
          case "Cache":
            const operationType = (operationData.verb == "GET_CACHED_RESULT") ? 'cache' : 'map';
            const cacheHeader =  `${capitalizeFirstLetter(operationData.systemProductName)} ${operationType}: ${operationData.connectionName}` // eg: Hazelcast operation cache | Hazelcast map
            // Hazelcast have requested that "Get cached result" become simply "Get" in the UI
            const displayVerb = (operationData.verb == "GET_CACHED_RESULT") ? "GET" : operationData.verb
            const verb = capitalizeFirstLetter(displayVerb.toLowerCase().replaceAll("_", " "))
            const cacheSubheader = `${verb}: ${operationData.cacheName}`
            return [cacheHeader, cacheSubheader];
        }
      }

      if (nodeType === 'QualifiedName' && nodeText.includes('@@')) {
        const [serviceName, operationName] = nodeText.split('@@');
        const serviceDisplayName = serviceName.split('.').pop();
        return [serviceDisplayName, `${serviceName} | Operation: ${operationName}`];
      } else if (nodeType === 'Expression') {
        return [nodeText, nodeText];
      } else {
        return [nodeText, null];
      }
    }

    rows.forEach((row, index) => {
      const sourceId = this.toSafeId(row.sourceNode);
      if (!nodes.has(sourceId)) {
        const debugging = row
        const [header, subheader] = getNodeLabel(row.sourceNode, row.sourceNodeType, row.sourceNodeOperationData);
        const [nodeType, nodeTypeLabel] = rowTypeToGraphType(row.sourceNodeType);
        nodes.set(sourceId, {
          id: sourceId,
          label: header,
          data: {
            nodeOperationData: row.sourceNodeOperationData,
            ...row
          },
          type: nodeType,
          subHeader: subheader,
          nodeId: sourceId,
          tooltip: nodeTypeLabel
        });
      }
      if (row.sourceNodeType === 'AttributeName') {
        attributeNodeIds.set(sourceId, sourceId);
      }

      const targetId = this.toSafeId(row.targetNode);
      if (!nodes.has(targetId)) {
        const [header, subheader] = getNodeLabel(row.targetNode, row.targetNodeType, row.targetNodeOperationData);
        const [nodeType, nodeTypeLabel] = rowTypeToGraphType(row.targetNodeType);
        nodes.set(targetId, {
          id: targetId,
          label: header,
          type: nodeType,
          data: {
            nodeOperationData: row.targetNodeOperationData,
            ...row
          },
          subHeader: subheader,
          nodeId: targetId,
          tooltip: nodeTypeLabel
        });
      }
      if (row.targetNodeType === 'AttributeName') {
        attributeNodeIds.set(targetId, targetId);
      }
      links.set(index, {
        source: sourceId,
        target: targetId,
        label: row.count.toString()
      });

    });
    const clusters = [
      {
        id: 'attributes',
        label: '',
        childNodeIds: Array.from(attributeNodeIds.keys())
      }
    ];
    return [new SchemaGraph(nodes, links).toNodeSet(), clusters];
  }

}

interface QueryLineageOperationalNode extends QuerySankeyChartRow {
  nodeOperationData?: SankeyOperationNodeDetails
}
