import {NgModule} from '@angular/core';

import {ServiceViewComponent} from './service-view.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {SearchModule} from '../search/search.module';
import {ServiceViewContainerComponent} from './service-view-container.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {DescriptionEditorModule} from '../type-viewer/description-editor/description-editor.module';
import {RouterModule} from '@angular/router';
import {LineageGraphModule} from '../type-viewer/lineage-graph/lineage-graph.module';

@NgModule({
    imports: [
        MatToolbarModule,
        SearchModule,
        CommonModule,
        BrowserModule,
        DescriptionEditorModule,
        RouterModule,
        LineageGraphModule
    ],
  exports: [ServiceViewContainerComponent, ServiceViewComponent],
  declarations: [ServiceViewComponent, ServiceViewContainerComponent],
  providers: [],
})
export class ServiceViewModule {
}
