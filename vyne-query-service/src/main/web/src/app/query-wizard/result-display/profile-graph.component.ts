import {Component, OnInit} from '@angular/core';
import {ProfilerOperation} from "../../services/query.service";
import * as shape from 'd3-shape';


@Component({
  selector: 'app-profile-graph',
  templateUrl: './profile-graph.component.html',
  styleUrls: ['./profile-graph.component.scss']
})
export class ProfileGraphComponent implements OnInit {

  profilerOperation: ProfilerOperation;

  objectKeys = Object.keys;

  graphNodes: ProfileOperationNode[] = [];
  graphLinks: NodeLink[] = [];

  graphLinkCurve: any = shape.curveLinear;

  graphColorScheme = {
    name: 'vivid',
    selectable: true,
    group: 'Ordinal',
    domain: [
      '#647c8a', '#3f51b5', '#2196f3', '#00b862', '#afdf0a', '#a7b61a', '#f3e562', '#ff9800', '#ff5722', '#ff4514'
    ]
  };

  constructor() {
  }

  ngOnInit() {
    this.createGraphNodes()
  }

  onLegendLabelClick(event) {
    console.log("Label click");
  }

  onNodeSelected(event) {
    console.log("Node selected");
  }

  createGraphNodes() {
    this.graphNodes = [];
    this.graphLinks = [];
    this.appendProfileOperationNodes(this.profilerOperation, null)
  }

  private appendProfileOperationNodes(operation: ProfilerOperation, parent: ProfilerOperation) {
    this.graphNodes.push(new ProfileOperationNode(operation.id, operation.description, operation));
    if (parent) {
      this.graphLinks.push(new NodeLink(parent.id, operation.id))
    }
    operation.children.forEach(child => this.appendProfileOperationNodes(child, operation))
  }


}

export class NodeLink {
  constructor(readonly source: string, readonly target: string, readonly label: string = null) {
  }
}

export class ProfileOperationNode {
  constructor(readonly id: string, readonly label: string, readonly operation: ProfilerOperation) {
  }
}
