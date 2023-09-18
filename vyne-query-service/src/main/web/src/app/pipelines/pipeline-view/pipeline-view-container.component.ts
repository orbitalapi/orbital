import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {PipelineService, RunningPipelineSummary, SubmittedPipeline} from '../pipelines.service';
import {flatMap, map, mergeMap} from 'rxjs/operators';
import {Observable} from 'rxjs/internal/Observable';
import {MatSnackBar} from '@angular/material/snack-bar';
import {TypesService} from '../../services/types.service';
import {Schema} from '../../services/schema';
import {MatDialog} from '@angular/material/dialog';
import {
  ConfirmationAction,
  ConfirmationDialogComponent,
  ConfirmationParams
} from '../../confirmation-dialog/confirmation-dialog.component';
import {AppInfoService, PipelineConfig, AppConfig} from '../../services/app-info.service';

@Component({
  selector: 'app-pipeline-view-container',
  template: `
    <app-pipeline-view [pipeline]="pipeline | async"
                       [schema]="schema | async"
                       (deletePipeline)="deletePipeline($event)"
                       [pipelineConfig]="config | async"
    ></app-pipeline-view>
  `,
  styleUrls: ['./pipeline-view-container.component.scss']
})
export class PipelineViewContainerComponent {
  pipeline: Observable<RunningPipelineSummary>;
  schema: Observable<Schema>;
  config: Observable<PipelineConfig>;

  constructor(private activatedRoute: ActivatedRoute,
              private pipelineService: PipelineService,
              private router: Router,
              private snackbar: MatSnackBar,
              private typeService: TypesService,
              private dialogService: MatDialog,
              private configService: AppInfoService
  ) {
    this.schema = typeService.getTypes();
    this.config = configService.getConfig().pipe(map(c => c.pipelineConfig));
    this.pipeline = this.activatedRoute.paramMap
      .pipe(mergeMap(params => {
        const pipelineId = params.get('pipelineId');
        return this.pipelineService.getPipeline(pipelineId);
      }));
  }

  deletePipeline(event: SubmittedPipeline) {
    this.dialogService.open(
      ConfirmationDialogComponent,
      {
        data: new ConfirmationParams(
          'Delete pipeline?',
          `This will remove the pipeline "${event.name}".  This action cannot be undone.`
        )
      }
    ).afterClosed().subscribe((result: ConfirmationAction) => {
      if (result === 'OK') {
        this.pipelineService.deletePipeline(event.pipelineSpecId)
          .subscribe(success => {
            this.snackbar.open(`Pipeline ${event.name} deleted`, 'Dismiss', {duration: 3000});
            this.router.navigate(['pipeline-manager']);
          }, error => {
            console.log(JSON.stringify(error));
            this.snackbar.open(`An error occurred, pipeline was not deleted`, 'Dismiss', {duration: 3000});
          });
      }
    });

  }
}
