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
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {isNullOrUndefined} from 'util';

@Component({
  selector: 'app-pipeline-view',
  template: `
    <div class="page-content" *ngIf="pipeline">
      <div class="page-heading">
        <h4>Pipeline</h4>
        <h1>{{pipeline.pipeline.name}}</h1>
      </div>
      <div class="row">
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

  pipelineTransportSpecFg: FormGroup;

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
          formControls[key] = new FormControl(transportSpec[key]);
        });
      return new FormGroup(formControls);
    }

    this.pipelineTransportSpecFg = new FormGroup({
      name: new FormControl(currentValue.name || '', Validators.required),
      input: new FormControl(currentValue.input, Validators.required),
      inputSpec: formGroupFromTransportSpec(currentValue.input),
      output: new FormControl(currentValue.output, Validators.required),
      outputSpec: formGroupFromTransportSpec(currentValue.output)
    });
  }

  onDeletePipelineClicked() {
    this.deletePipeline.emit(this.pipeline.pipeline);
  }
}
