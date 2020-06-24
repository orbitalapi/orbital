import {Component, Input, OnInit} from '@angular/core';
import {Schema} from '../../services/schema';

@Component({
  selector: 'app-pipeline-wizard',
  templateUrl: './pipeline-wizard.component.html',
  styleUrls: ['./pipeline-wizard.component.scss']
})
export class PipelineWizardComponent {

  @Input()
  schema: Schema;

}
