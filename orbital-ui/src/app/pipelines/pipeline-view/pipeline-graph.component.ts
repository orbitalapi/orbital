import {ChangeDetectionStrategy, Component, ElementRef, Input, ViewChild} from '@angular/core';
import {DagDataset} from '../pipelines.service';
import * as shape from 'd3-shape';
import {BaseGraphComponent} from '../../inheritence-graph/base-graph-component';

@Component({
  selector: 'app-pipeline-graph',
  templateUrl: 'pipeline-graph.component.html',
  styleUrls: ['./pipeline-graph.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PipelineGraphComponent extends BaseGraphComponent {

  @Input()
  graph: DagDataset;


  @ViewChild('chartOuterContianer', {static: true})
  chartContainer: ElementRef;


  showLegend = false;
  curve = shape.curveBundle.beta(1);
  // chartDimensions = [1400, 800];
  autoZoom = false;

}
