import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {
  MetricValueSet,
  PipelineService,
  PipelineSpec,
  PipelineTransportSpec,
  RunningPipelineSummary, SubmittedPipeline
} from '../pipelines.service';
import {Schema} from '../../services/schema';
import {UntypedFormControl, UntypedFormGroup, Validators} from '@angular/forms';
import {isNullOrUndefined} from 'util';
import {PipelineConfig} from '../../services/app-info.service';

@Component({
  selector: 'app-pipeline-view',
  template: `
    <div class="page-content" *ngIf="pipeline">
      <div class="page-heading">
        <h4>Pipeline</h4>
        <h1>{{pipeline.pipeline.name}}</h1>
      </div>
      <div class="button-row">
        <button mat-stroked-button *ngIf="pipelineConfig?.kibanaUrl" (click)="openLogs()">
          View logs
        </button>
        <div class="spacer"></div>
        <button mat-flat-button color="warn" (click)="onDeletePipelineClicked()">
          Delete pipeline
        </button>
      </div>
      <div class="stats-row">
        <app-statistic
          label="Submitted"
          [value]="pipeline.status.submissionTime | amCalendar"
        ></app-statistic>
        <app-statistic
          label="Status"
          [value]="pipeline.status.status | titlecase"
        ></app-statistic>
        <app-statistic
          label="Received"
          [value]="latestValue(pipeline.status.metrics.receivedCount)"
        ></app-statistic>
        <app-statistic
          label="Emitted"
          [value]="latestValue(pipeline.status.metrics.emittedCount)"
        ></app-statistic>
        <app-statistic
          label="In-flight"
          [value]="latestValue(pipeline.status.metrics.inflight)"
        ></app-statistic>
        <app-statistic
          label="Queued"
          [value]="latestValue(pipeline.status.metrics.queueSize)"
        ></app-statistic>
      </div>

<!--      <app-pipeline-graph [graph]="pipeline.pipeline.graph"></app-pipeline-graph>-->
      <div class="transport-config-row">
        <app-input-editor [schema]="schema"
                          [editable]="false"
                          [label]="'Input: ' + (pipeline.pipeline.spec.input.type)"
                          [pipelineTransportSpecFg]="pipelineTransportSpecFg.get('inputSpec')"
                          [transportSpecType]="pipeline.pipeline.spec.input.type"
                          [pipelineTransportSpec]="pipeline.pipeline.spec.input"></app-input-editor>
        <app-output-editor [schema]="schema"
                           [editable]="false"
                           [label]="'Output: ' + (pipeline.pipeline.spec.output.type)"
                           [pipelineTransportSpecFg]="pipelineTransportSpecFg.get('outputSpec')"
                           [transportSpecType]="pipeline.pipeline.spec.output.type"
                           [pipelineTransportSpec]="pipeline.pipeline.spec.output"></app-output-editor>
      </div>

    </div>
  `,
  styleUrls: ['./pipeline-view.component.scss']
})
export class PipelineViewComponent {
  private _pipeline: RunningPipelineSummary;

  pipelineTransportSpecFg: UntypedFormGroup;

  @Input()
  pipelineConfig: PipelineConfig;

  @Output()
  deletePipeline = new EventEmitter<SubmittedPipeline>();

  @Input()
  get pipeline(): RunningPipelineSummary {
    return this._pipeline;
  }

  set pipeline(value: RunningPipelineSummary) {
    this._pipeline = value;
    this.buildDefaultFormGroupControls();
  }

  @Input()
  schema: Schema;

  latestValue(valueSet: MetricValueSet[]): string {
    if (valueSet.length === 0) {
      return '--';
    }
    return '' + valueSet[0].latestValue.value;
  }

  private buildDefaultFormGroupControls() {
    if (isNullOrUndefined(this.pipeline)) {
      return;
    }
    const currentValue = this.pipeline.pipeline.spec;

    function formGroupFromTransportSpec(transportSpec: PipelineTransportSpec) {
      const formControls = {};
      Object.keys(transportSpec)
        // These values aren't configurable within this component, so exclude
        // from the data we collect / emit
        .filter(k => k !== 'type' && k !== 'direction')
        .forEach(key => {
          formControls[key] = new UntypedFormControl(transportSpec[key]);
        });
      return new UntypedFormGroup(formControls);
    }

    this.pipelineTransportSpecFg = new UntypedFormGroup({
      name: new UntypedFormControl(currentValue.name || '', Validators.required),
      input: new UntypedFormControl(currentValue.input, Validators.required),
      inputSpec: formGroupFromTransportSpec(currentValue.input),
      output: new UntypedFormControl(currentValue.output, Validators.required),
      outputSpec: formGroupFromTransportSpec(currentValue.output)
    });
  }

  onDeletePipelineClicked() {
    this.deletePipeline.emit(this.pipeline.pipeline);
  }

  openLogs() {
    //http://localhost:5601/app/discover#/?_
    // g=(filters:!(),refreshInterval:(pause:!t,value:0),
    // time:(from:now-15m,to:now))&_a=(columns:!(level,message),filters:!(),
    // grid:(columns:(level:(width:117))),
    // hideChart:!t,index:'302a6cb0-040f-11ec-a473-775543ab603f',interval:auto,query:(language:kuery,query:'06b1-12ff-ab80-0001'),sort:!(!('@timestamp',desc)))
    const logsKibanaQuery = `?_g=(filters:!(),refreshInterval:(pause:!t,value:0),` +
      `time:(from:now-15m,to:now))&_a=(columns:!(level,message),filters:!(),` +
      `grid:(columns:(level:(width:117))),hideChart:!t,` +
      `index:'${this.pipelineConfig.logsIndex}',` +
      `interval:auto,query:(language:kuery,query:'${this.pipeline.pipeline.jobId}'),sort:!(!('@timestamp',desc)))`;
    const kibanaUrl = this.pipelineConfig.kibanaUrl.endsWith('/') ? this.pipelineConfig.kibanaUrl : this.pipelineConfig.kibanaUrl + '/';
    window.open(`${kibanaUrl}app/discover#` + logsKibanaQuery, '_blank');
  }
}
