import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {MatLegacyFormFieldModule as MatFormFieldModule} from '@angular/material/legacy-form-field';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatLegacyAutocompleteModule as MatAutocompleteModule} from '@angular/material/legacy-autocomplete';
import {MatLegacySlideToggleModule as MatSlideToggleModule} from '@angular/material/legacy-slide-toggle';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {PipelineBuilderComponent} from './pipeline-builder/pipeline-builder.component';
import {MatLegacyInputModule as MatInputModule} from '@angular/material/legacy-input';
import {VyneFormsModule} from '../forms/vyne-forms.module';
import {MatLegacySelectModule as MatSelectModule} from '@angular/material/legacy-select';
import {PollingOperationInputConfigComponent} from './pipeline-builder/polling-operation-input-config.component';
import {TransportSelectorComponent} from './pipeline-builder/transport-selector.component';
import {HttpListenerInputConfigComponent} from './pipeline-builder/http-listener-input-config.component';
import {KafkaTopicConfigComponent} from './pipeline-builder/kafka-topic-config.component';
import {OperationOutputConfigComponent} from './pipeline-builder/operation-output-config.component';
import {PipelineManagerComponent} from './pipeline-manager/pipeline-manager.component';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {PipelineListComponent} from './pipeline-list/pipeline-list.component';
import {RouterModule} from '@angular/router';
import {MatLegacyProgressBarModule as MatProgressBarModule} from '@angular/material/legacy-progress-bar';
import {PipelineViewComponent} from './pipeline-view/pipeline-view.component';
import {PipelineViewContainerComponent} from './pipeline-view/pipeline-view-container.component';
import {StatisticModule} from '../statistic/statistic.module';
import {MomentModule} from 'ngx-moment';
import {PipelineGraphComponent} from './pipeline-view/pipeline-graph.component';
import {NgxGraphModule} from '@swimlane/ngx-graph';
import {InputEditorComponent} from './pipeline-builder/input-editor.component';
import {OutputEditorComponent} from './pipeline-builder/output-editor.component';
import {MatLegacyDialogModule as MatDialogModule} from '@angular/material/legacy-dialog';
import {PipelineBuilderContainerComponent} from './pipeline-builder/pipeline-builder-container.component';
import {SchemaDisplayTableModule} from '../schema-display-table/schema-display-table.module';
import {TuiButtonModule, TuiDataListModule, TuiSvgModule} from '@taiga-ui/core';
import {SqsS3InputConfigComponent} from './pipeline-builder/sqs-s3-input-config.component';
import {JdbcOutputConfigComponent} from './pipeline-builder/jdbc-output-config.component';
import {TuiComboBoxModule} from '@taiga-ui/kit';
import {ConnectionFiltersModule} from '../utils/connections.pipe';
import {
  PollingScheduleFormInputComponent
} from './pipeline-builder/polling-schedule-form-input/polling-schedule-form-input.component';
import {PollingQueryInputConfigComponent} from './pipeline-builder/polling-query-input-config.component';
import {AwsS3OutputConfigComponent} from './pipeline-builder/aws-s3-output-config.component';
import {AuthGuard} from 'src/app/services/auth.guard';
import {VynePrivileges} from 'src/app/services/user-info.service';
import {PipelineService} from 'src/app/pipelines/pipelines.service';
import {VyneServicesModule} from 'src/app/services/vyne-services.module';

@NgModule({
  declarations: [
    PipelineBuilderComponent,
    PollingOperationInputConfigComponent,
    PollingScheduleFormInputComponent,
    PollingQueryInputConfigComponent,
    TransportSelectorComponent,
    TransportSelectorComponent,
    HttpListenerInputConfigComponent,
    KafkaTopicConfigComponent,
    OperationOutputConfigComponent,
    PipelineManagerComponent,
    PipelineListComponent,
    PipelineViewComponent,
    PipelineViewContainerComponent,
    PipelineGraphComponent,
    InputEditorComponent,
    OutputEditorComponent,
    PipelineBuilderContainerComponent,
    SqsS3InputConfigComponent,
    JdbcOutputConfigComponent,
    AwsS3OutputConfigComponent
  ],
  imports: [
    CommonModule,
    TypeAutocompleteModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    ReactiveFormsModule,
    VyneFormsModule,
    MatInputModule,
    VyneFormsModule,
    MatSelectModule,
    MatAutocompleteModule,
    HeaderBarModule,
    RouterModule,
    MatProgressBarModule,
    StatisticModule,
    MomentModule,
    NgxGraphModule,
    SchemaDisplayTableModule,
    TuiButtonModule,
    TuiComboBoxModule,
    TuiSvgModule,
    TuiDataListModule,
    FormsModule,
    ConnectionFiltersModule,
    VyneServicesModule,
    RouterModule.forChild([
      {
        path: '', component: PipelineManagerComponent, children: [
          {
            path: '',
            component: PipelineListComponent,
            canActivate: [AuthGuard],
            data: { requiredAuthority: VynePrivileges.ViewPipelines }
          },
          {
            path: 'new',
            component: PipelineBuilderContainerComponent,
            canActivate: [AuthGuard],
            data: { requiredAuthority: VynePrivileges.EditPipelines }
          },
          {
            path: ':pipelineId',
            component: PipelineViewContainerComponent,
            canActivate: [AuthGuard],
            data: { requiredAuthority: VynePrivileges.EditPipelines }
          }
        ]
      }
    ])
  ],
  exports: [
    PipelineBuilderComponent,
    PipelineListComponent,
    PipelineManagerComponent,
    PipelineViewComponent
  ],
  providers: [
    PipelineService,
  ]
})
export class PipelinesModule {
}
