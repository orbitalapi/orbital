import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {AppModule} from '../app.module';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {MatFormFieldModule} from '@angular/material/form-field';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatButtonModule} from '@angular/material/button';
import {PipelineBuilderComponent} from './pipeline-builder/pipeline-builder.component';
import {MatInputModule} from '@angular/material/input';
import {VyneFormsModule} from '../forms/vyne-forms.module';
import {NgSelectModule} from '@ng-select/ng-select';
import {MatSelectModule} from '@angular/material/select';
import {PollingInputConfigComponent} from './pipeline-builder/polling-input-config.component';
import {TransportSelectorComponent} from './pipeline-builder/transport-selector.component';
import {HttpListenerInputConfigComponent} from './pipeline-builder/http-listener-input-config.component';
import {KafkaTopicConfigComponent} from './pipeline-builder/kafka-topic-config.component';
import {CaskOutputConfigComponent} from './pipeline-builder/cask-output-config.component';
import {OperationOutputConfigComponent} from './pipeline-builder/operation-output-config.component';
import {PipelineManagerComponent} from './pipeline-manager/pipeline-manager.component';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {PipelineListComponent} from './pipeline-list/pipeline-list.component';
import {RouterModule} from '@angular/router';
import {MatProgressBarModule} from '@angular/material/progress-bar';

@NgModule({
  declarations: [PipelineBuilderComponent, PollingInputConfigComponent, TransportSelectorComponent, TransportSelectorComponent, HttpListenerInputConfigComponent, KafkaTopicConfigComponent, CaskOutputConfigComponent, OperationOutputConfigComponent, PipelineManagerComponent, PipelineListComponent],
  imports: [
    CommonModule,
    TypeAutocompleteModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatFormFieldModule,
    ReactiveFormsModule,
    VyneFormsModule,
    MatInputModule,
    VyneFormsModule,
    NgSelectModule,
    MatSelectModule,
    MatAutocompleteModule,
    HeaderBarModule,
    RouterModule,
    MatProgressBarModule
  ],
  exports: [
    PipelineBuilderComponent,
    PipelineListComponent,
    PipelineManagerComponent
  ]
})
export class PipelinesModule {
}
