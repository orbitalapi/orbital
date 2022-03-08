import {Component, OnInit} from '@angular/core';
import {TypesService} from '../../services/types.service';
import {PipelineService, PipelineSpec, PipelineStateSnapshot} from '../pipelines.service';
import {Schema} from '../../services/schema';
import {ConnectorSummary, DbConnectionService} from "../../db-connection-editor/db-importer.service";

@Component({
  selector: 'app-pipeline-builder-container',
  template: `
    <app-pipeline-builder
      [schema]="schema"
      [connections]="connections"
      (createPipeline)="createPipeline($event)"
      [working]="working"
      [pipelineStatus]="pipelineStatus"
      [pipelineErrorMessage]="pipelineErrorMessage"

    ></app-pipeline-builder>
  `,
  styleUrls: ['./pipeline-builder-container.component.scss']
})
export class PipelineBuilderContainerComponent {

  schema: Schema;
  connections: ConnectorSummary[];
  working = false;
  pipelineStatus: PipelineStateSnapshot;
  pipelineErrorMessage: string;
  constructor(private typeService: TypesService, private pipelineService: PipelineService, private dbConnectionService: DbConnectionService) {
    this.typeService.getTypes()
      .subscribe(s => this.schema = s);

    this.dbConnectionService.getConnections().subscribe(connections => this.connections = connections)
  }


  createPipeline(pipelineSpec: PipelineSpec) {
    this.working = true;
    this.pipelineErrorMessage = null;
    this.pipelineService.submitPipeline(pipelineSpec)
      .subscribe(result => {
        this.working = false;
        this.pipelineStatus = result;
      }, error => {
        this.working = false;
        this.pipelineErrorMessage = error.error.message;
      });
  }
}
