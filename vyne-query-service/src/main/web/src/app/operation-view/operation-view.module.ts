import {NgModule} from '@angular/core';
import {OperationViewComponent} from './operation-view.component';
import {OperationViewContainerComponent} from './operation-view-container.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {SearchModule} from '../search/search.module';
import {DescriptionEditorModule} from '../type-viewer/description-editor/description-editor.module';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {MatLegacyButtonModule as MatButtonModule} from '@angular/material/legacy-button';
import {MatLegacyInputModule as MatInputModule} from '@angular/material/legacy-input';
import {QueryPanelModule} from '../query-panel/query-panel.module';
import {OperationErrorComponent} from './operation-error.component';
import {ObjectViewModule} from '../object-view/object-view.module';
import {MatLegacyProgressSpinnerModule as MatProgressSpinnerModule} from '@angular/material/legacy-progress-spinner';
import {TuiLinkModule} from '@taiga-ui/core';
import {TuiToggleModule} from '@taiga-ui/kit';
import {FormsModule} from '@angular/forms';
import {MarkdownModule} from "ngx-markdown";


@NgModule({
  imports: [
    CommonModule,
    MatToolbarModule,
    SearchModule,
    DescriptionEditorModule,
    RouterModule,
    MatButtonModule,
    MatInputModule,
    QueryPanelModule,
    ObjectViewModule,
    MatProgressSpinnerModule,
    TuiLinkModule,
    TuiToggleModule,
    FormsModule,
    MarkdownModule
  ],
  exports: [OperationViewComponent, OperationViewContainerComponent],
  declarations: [OperationViewComponent, OperationViewContainerComponent, OperationErrorComponent],
  providers: [],
})
export class OperationViewModule {
}
