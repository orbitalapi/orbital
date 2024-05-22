import {Component} from '@angular/core';
import {PipelineService, RunningPipelineSummary} from '../pipelines.service';
import {MatSnackBar} from '@angular/material/snack-bar';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-pipeline-list',
  template: `
    <h2>Pipeline manager</h2>
    <div *ngIf="pipelines.length; else empty">
      <p>
        Pipelines can transport, transform and enrich data automatically for you.
      </p>
      <div class="page-button-row">
        <button tuiButton size="m" (click)="createNewPipeline()" appearance="secondary">Create new pipeline</button>
      </div>
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
      <div class="empty-state-container">
        <img src="assets/img/illustrations/pipeline.svg">
        <p>
          Pipelines can transport, transform and enrich data automatically for you. Create a new pipeline to get
          started.
        </p>
        <button tuiButton size="l" appearance="primary" (click)="createNewPipeline()">Create new pipeline</button>
      </div>
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

  createNewPipeline() {
    this.router.navigate(['new'], {relativeTo: this.activatedRoute})
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
