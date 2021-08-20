import {Component, Input, OnInit} from '@angular/core';
import {Schema} from '../../services/schema';
import {AbstractControl, FormControl, FormGroup, Validators} from '@angular/forms';
import {
  PIPELINE_INPUTS,
  PIPELINE_OUTPUTS, PipelineService,
  PipelineSpec, PipelineStateSnapshot,
  PipelineTransport, PipelineTransportSpec,
  PipelineTransportType
} from '../pipelines.service';
import {TypesService} from '../../services/types.service';

@Component({
  selector: 'app-pipeline-builder',
  templateUrl: './pipeline-builder.component.html',
  styleUrls: ['./pipeline-builder.component.scss']
})
export class PipelineBuilderComponent implements OnInit {

  constructor(private typeService: TypesService, private pipelineService: PipelineService) {
    this.typeService.getTypes()
      .subscribe(s => this.schema = s);
  }

  @Input()
  schema: Schema;

  pipelineSources = PIPELINE_INPUTS;
  pipelineTargets = PIPELINE_OUTPUTS;

  pipelineSpecFg: FormGroup;

  working = false;
  pipelineStatus: PipelineStateSnapshot;
  pipelineErrorMessage: string;

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

  createPipeline() {
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
    this.pipelineService.submitPipeline(pipelineSpec)
      .subscribe(result => {
        this.working = false;
        this.pipelineStatus = result;
      }, error => {
        this.working = false;
        this.pipelineErrorMessage = error.error.message;
      });
  }


  ngOnInit(): void {
    this.buildDefaultFormGroupControls();
  }

  updateConfigValue(config: any, pipelineTargetFormControl: AbstractControl) {
    pipelineTargetFormControl.setValue(config);
  }
}

