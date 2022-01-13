import {NgModule} from '@angular/core';
import {OperationViewComponent} from './operation-view.component';
import {OperationViewContainerComponent} from './operation-view-container.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {SearchModule} from '../search/search.module';
import {DescriptionEditorModule} from '../type-viewer/description-editor/description-editor.module';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {RouterModule} from '@angular/router';
import {MatButtonModule} from '@angular/material/button';
import {MatInputModule} from '@angular/material/input';
import {QueryPanelModule} from '../query-panel/query-panel.module';
import { OperationErrorComponent } from './operation-error.component';
import {ObjectViewModule} from '../object-view/object-view.module';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {TuiLinkModule} from '@taiga-ui/core';
import {TuiToggleModule} from '@taiga-ui/kit';
import {FormsModule} from '@angular/forms';


@NgModule({
  imports: [
    CommonModule,
    BrowserModule,
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
    FormsModule
  ],
  exports: [OperationViewComponent, OperationViewContainerComponent],
  declarations: [OperationViewComponent, OperationViewContainerComponent, OperationErrorComponent],
  providers: [],
})
export class OperationViewModule {
}
