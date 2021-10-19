import {Component} from '@angular/core';
import * as shape from 'd3-shape';
import {outerRectangle, innerRectangle} from './graph-utils';
import {schema} from './sample';
import {SchemaGraph} from './type-link-graph/schema';
import {Observable} from 'rxjs/internal/Observable';
import {of} from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'graph-vis';

  graphSource: string = null;

  graph = of(SchemaGraph.from(schema.nodes, schema.links));

  curve = shape.curveBundle.beta(1);
  colors = {
    TYPE: '#66BD6D',
    MEMBER: '#FA783B',
    OPERATION: '#55ACD2'
  };

  colorScheme = {
    domain: [
      '#FAC51D',
      '#66BD6D',
      '#FAA026',
      '#29BB9C',
      // '#E96B56',
      '#55ACD2',
      // '#B7332F',
      // '#2C83C9',
      // '#9166B8',
      '#92E7E8',
      '#16aa6d',
      '#aebfc9'
    ]
  };
  orientation = 'LR';


  getStroke(node) {
    const nodeType = node.type;

    if (!this.colors[nodeType]) {
      console.log('No color defined for node type ' + nodeType);
    }
    return this.colors[nodeType] || '#FAC51D';
  }

  outerRectangle(width: number, height: number): string {
    return outerRectangle(width, height);
  }

  innerRectangle(width: number, height: number): string {
    return innerRectangle(width, height);
  }

  onSchemaUpdated($event: string) {
    this.graphSource = $event;
    const displayGraph = JSON.parse($event);
    this.graph = of(SchemaGraph.from(displayGraph.nodes, displayGraph.links));
  }

  useSample() {
    this.graph = of(SchemaGraph.from(schema.nodes, schema.links));
  }
}
