import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {findType, QualifiedName, Schema, Type} from '../../services/schema';
import {AbstractControl, FormControl, FormGroup, Validators} from '@angular/forms';
import {
  PIPELINE_INPUTS,
  PIPELINE_OUTPUTS, PipelineService,
  PipelineSpec, PipelineStateSnapshot,
  PipelineTransport, PipelineTransportSpec,
  PipelineTransportType
} from '../pipelines.service';
import {TypesService} from '../../services/types.service';
import {ConnectorSummary} from "../../db-connection-editor/db-importer.service";

@Component({
  selector: 'app-pipeline-builder',
  templateUrl: './pipeline-builder.component.html',
  styleUrls: ['./pipeline-builder.component.scss']
})
export class PipelineBuilderComponent implements OnInit {

  @Input()
  schema: Schema;

  @Input()
  connections: ConnectorSummary[];

  pipelineSources = PIPELINE_INPUTS;
  pipelineTargets = PIPELINE_OUTPUTS;

  pipelineSpecFg: FormGroup;

  @Input()
  working = false;

  inputType: Type;
  outputType: Type;

  @Input()
  pipelineStatus: PipelineStateSnapshot;

  @Input()
  pipelineErrorMessage: string;

  @Output()
  createPipeline = new EventEmitter<PipelineSpec>();

  private buildDefaultFormGroupControls() {
    const currentValue = (this.pipelineSpecFg) ?
      this.pipelineSpecFg.getRawValue() :
      {};
    this.pipelineSpecFg = new FormGroup({
      name: new FormControl(currentValue.pipelineName || '', Validators.required),
      input: new FormControl(currentValue.input, Validators.required),
      inputSpec: new FormControl(),
      output: new FormControl(currentValue.output, Validators.required),
      outputSpec: new FormControl()
    });
  }

  pipelineTransportLabel(transport: PipelineTransport): string {
    return transport ? transport.label : '';
  }

  emitCreatePipelineEvent() {
    const formData = this.pipelineSpecFg.getRawValue();
    console.log(JSON.stringify(this.pipelineSpecFg.getRawValue()));
    const pipelineSpec: PipelineSpec = {
      name: formData.name,
      input: {
        type: formData.input.type,
        direction: 'INPUT',
        ...formData.inputSpec
      },
      output: {
        type: formData.output.type,
        direction: 'OUTPUT',
        ...formData.outputSpec
      }
    };
    console.log(JSON.stringify(pipelineSpec));
    this.working = true;
    this.createPipeline.emit(pipelineSpec);

  }


  ngOnInit(): void {
    this.buildDefaultFormGroupControls();
  }

  updateConfigValue(config: any, pipelineTargetFormControl: AbstractControl) {
    pipelineTargetFormControl.setValue(config);
  }

  handleInputPayloadTypeChanged($event: QualifiedName) {
    this.inputType = $event ? findType(this.schema, $event.parameterizedName) : null;
  }

  handleOutputPayloadTypeChanged($event: QualifiedName) {
    this.outputType = $event ? findType(this.schema, $event.parameterizedName) : null;
  }
}

