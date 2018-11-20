import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TaxiViewerComponent} from "./taxi-viewer.component";
import {CovalentHighlightModule} from "@covalent/highlight";
import {MatButtonModule, MatIconModule, MatProgressBarModule} from "@angular/material";
import {CodeContainerComponent} from "./code-container.component";
import {CallExplorerComponent} from './call-explorer/call-explorer.component';
import {OperationViewComponent} from './call-explorer/operation-view.component';
import {HttpClientModule} from "@angular/common/http";
import {VyneQueryViewerComponent} from './vyne-query-viewer/vyne-query-viewer.component';
import {MermaidComponent} from "./sequence-diagram/mermaid.component";
import {SequenceDiagramComponent} from "./sequence-diagram/sequence-diagram.component";

@NgModule({
  imports: [
    CommonModule,
    CovalentHighlightModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,

    HttpClientModule
  ],
  declarations: [
    TaxiViewerComponent,
    CodeContainerComponent,
    CallExplorerComponent,
    OperationViewComponent,
    VyneQueryViewerComponent,
    MermaidComponent,
    SequenceDiagramComponent,

  ],
  exports: [TaxiViewerComponent,
    CallExplorerComponent,
    CodeContainerComponent
  ]
})
export class TaxiViewerModule {
}
