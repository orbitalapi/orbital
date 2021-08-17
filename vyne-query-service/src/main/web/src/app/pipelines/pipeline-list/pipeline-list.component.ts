import {Component, OnInit} from '@angular/core';
import {PipelineService, PipelineStateSnapshot} from '../pipelines.service';
import {Observable} from 'rxjs/internal/Observable';
import {of} from 'rxjs';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-pipeline-list',
  template: `
    <div class="container" *ngIf="pipelines.length; else empty">
      <table class="pipeline-list">
        <thead>
        <tr>
          <th>Pipeline name</th>
<!--          <th>Description</th>-->
          <th>State</th>
<!--          <th>Info</th>-->
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let pipeline of pipelines">
          <td>{{pipeline.name}}</td>
<!--          <td>{{pipeline.pipelineDescription}}</td>-->
          <td>{{pipeline.state}}</td>
<!--          <td>{{pipeline.info}}</td>-->
          <td>
            <button mat-stroked-button (click)="deletePipeline(pipeline)">Delete</button>
          </td>
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
  pipelines: PipelineStateSnapshot[] = [];
  errorMessage: string;

  constructor(private pipelineService: PipelineService, private snackbar: MatSnackBar) {
    this.reloadPipelines();
  }

  private reloadPipelines() {
    this.pipelineService.listPipelines()
      .subscribe(pipelines => this.pipelines = pipelines);
  }

  deletePipeline(pipeline: PipelineStateSnapshot) {
    this.errorMessage = null;
    this.pipelineService.deletePipeline(pipeline.name)
      .subscribe(success => {
          this.snackbar.open(`Pipeline ${pipeline.name} deleted successfully`, 'Dismiss', {duration: 3000});
          this.reloadPipelines();
        },
        error => {
          this.errorMessage = 'Failed to delete the pipeline: ' + (error.error.message || error.error.error);
        }
      );
  }
}
