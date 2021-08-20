import {Component, OnInit} from '@angular/core';
import {PipelineService, PipelineStateSnapshot, RunningPipelineSummary} from '../pipelines.service';
import {Observable} from 'rxjs/internal/Observable';
import {of} from 'rxjs';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-pipeline-list',
  template: `
    <div class="container" *ngIf="pipelines.length; else empty">
      <table class="pipeline-list">
        <thead>
        <tr>
          <th>Pipeline name</th>
          <th>Description</th>
          <th>State</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let pipelineSummary of pipelines" (click)="onPipelineSelected(pipelineSummary)">
          <td>{{pipelineSummary.pipeline.name}}</td>
          <td>{{pipelineSummary.pipeline.spec.description}}</td>
          <td>{{pipelineSummary.status.status.replace('_', ' ') | titlecase}}</td>
        </tr>
        </tbody>
      </table>
      <div class="test-result-box error-message" *ngIf="errorMessage">{{ errorMessage }}</div>
    </div>
    <ng-template #loading>
      Loading pipelines...
    </ng-template>
    <ng-template #empty>
      It's a little empty here. <a [routerLink]="['new']">Create a new pipeline</a> to get started,
    </ng-template>
  `,
  styleUrls: ['./pipeline-list.component.scss']
})
export class PipelineListComponent {
  pipelines: RunningPipelineSummary[] = [];
  errorMessage: string;

  constructor(private pipelineService: PipelineService, private snackbar: MatSnackBar, private activatedRoute: ActivatedRoute, private router: Router) {
    this.reloadPipelines();
  }

  private reloadPipelines() {
    this.pipelineService.listPipelines()
      .subscribe(pipelines => this.pipelines = pipelines);
  }

  deletePipeline(pipeline: RunningPipelineSummary) {
    this.errorMessage = null;
    this.pipelineService.deletePipeline(pipeline.pipeline.pipelineSpecId)
      .subscribe(success => {
          this.snackbar.open(`Pipeline ${pipeline.pipeline.name} deleted successfully`, 'Dismiss', {duration: 3000});
          this.reloadPipelines();
        },
        error => {
          this.errorMessage = 'Failed to delete the pipeline: ' + (error.error.message || error.error.error);
        }
      );
  }

  onPipelineSelected(pipelineSummary: RunningPipelineSummary) {
    this.router.navigate([pipelineSummary.pipeline.pipelineSpecId], {relativeTo: this.activatedRoute});
  }
}
