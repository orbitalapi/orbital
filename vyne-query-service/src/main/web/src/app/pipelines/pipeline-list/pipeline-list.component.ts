import {Component, OnInit} from '@angular/core';
import {PipelineService, PipelineStateSnapshot} from '../pipelines.service';
import {Observable} from 'rxjs/internal/Observable';

@Component({
  selector: 'app-pipeline-list',
  template: `
    <div class="container">
      <table class="pipeline-list">
        <thead>
        <tr>
          <th>Pipeline name</th>
          <th>Description</th>
          <th>State</th>
          <th>Info</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let pipeline of pipelines | async">
          <td>{{pipeline.name}}</td>
          <td>{{pipeline.pipelineDescription}}</td>
          <td>{{pipeline.state}}</td>
          <td>{{pipeline.info}}</td>
        </tr>
        </tbody>
      </table>
    </div>
  `,
  styleUrls: ['./pipeline-list.component.scss']
})
export class PipelineListComponent {
  private pipelines: Observable<PipelineStateSnapshot[]>;


  constructor(private pipelineService: PipelineService) {
    this.pipelines = pipelineService.listPipelines();
  }

}
