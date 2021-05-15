import {NgModule} from '@angular/core';
import {CallExplorerComponent} from './call-explorer.component';
import {CallExplorerOperationViewComponent} from './call-explorer-operation-view.component';
import {SequenceDiagramModule} from '../sequence-diagram/sequence-diagram.module';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatIconModule} from '@angular/material/icon';
import {ServiceGraphModule} from '../service-graph/service-graph.module';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {SimpleCodeViewerModule} from '../../../simple-code-viewer/simple-code-viewer.module';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatButtonModule} from '@angular/material/button';
import {ErrorBarModule} from '../../../error-message-display/error-bar.module';
import {RouterModule} from '@angular/router';

@NgModule({
    imports: [
        SequenceDiagramModule,
        MatButtonToggleModule,
        MatIconModule,
        ServiceGraphModule,
        CommonModule,
        BrowserModule,
        SimpleCodeViewerModule,
        MatTooltipModule,
        MatButtonModule,
        ErrorBarModule,
        RouterModule,
    ],
  exports: [CallExplorerComponent, CallExplorerOperationViewComponent],
  declarations: [CallExplorerComponent, CallExplorerOperationViewComponent],
  providers: [],
})
export class CallExplorerModule {
}
