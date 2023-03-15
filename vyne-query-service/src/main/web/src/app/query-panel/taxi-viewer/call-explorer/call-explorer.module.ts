import { NgModule } from '@angular/core';
import { CallExplorerComponent } from './call-explorer.component';
import { CallExplorerOperationViewComponent } from './call-explorer-operation-view.component';
import { SequenceDiagramModule } from '../sequence-diagram/sequence-diagram.module';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { ServiceGraphModule } from '../service-graph/service-graph.module';
import { CommonModule } from '@angular/common';
import { SimpleCodeViewerModule } from '../../../simple-code-viewer/simple-code-viewer.module';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { ErrorBarModule } from '../../../error-message-display/error-bar.module';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ServiceStatsModule } from '../../../service-stats/service-stats.module';
import { LineageDisplayModule } from '../../../lineage-display/lineage-display.module';
import { JsonViewerModule } from 'src/app/json-viewer/json-viewer.module';
import { ExpandingPanelSetModule } from 'src/app/expanding-panelset/expanding-panel-set.module';
import { AngularSplitModule } from 'angular-split';
import { FileSizePipeModule } from 'src/app/file-size-pipe/file-size-pipe.module';

@NgModule({
  imports: [
    SequenceDiagramModule,
    MatButtonToggleModule,
    MatIconModule,
    ServiceGraphModule,
    CommonModule,
    SimpleCodeViewerModule,
    MatTooltipModule,
    MatButtonModule,
    ErrorBarModule,
    RouterModule,
    FormsModule,
    ServiceStatsModule,
    LineageDisplayModule,
    JsonViewerModule,
    ExpandingPanelSetModule,
    AngularSplitModule,
    FileSizePipeModule,
  ],
  exports: [CallExplorerComponent, CallExplorerOperationViewComponent],
  declarations: [CallExplorerComponent, CallExplorerOperationViewComponent],
  providers: [],
})
export class CallExplorerModule {
}
