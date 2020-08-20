import {NgModule} from '@angular/core';
import {CallExplorerComponent} from './call-explorer.component';
import {OperationViewComponent} from './operation-view.component';
import {SequenceDiagramModule} from '../sequence-diagram/sequence-diagram.module';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatIconModule} from '@angular/material/icon';
import {ServiceGraphModule} from '../service-graph/service-graph.module';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {SimpleCodeViewerModule} from '../../../simple-code-viewer/simple-code-viewer.module';

@NgModule({
  imports: [
    SequenceDiagramModule,
    MatButtonToggleModule,
    MatIconModule,
    ServiceGraphModule,
    CommonModule,
    BrowserModule,
    SimpleCodeViewerModule
  ],
  exports: [CallExplorerComponent, OperationViewComponent],
  declarations: [CallExplorerComponent, OperationViewComponent],
  providers: [],
})
export class CallExplorerModule {
}
