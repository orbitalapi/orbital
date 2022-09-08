import { NgModule } from '@angular/core';

import { ServiceViewComponent } from './service-view.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { SearchModule } from '../search/search.module';
import { ServiceViewContainerComponent } from './service-view-container.component';
import { CommonModule } from '@angular/common';
import { DescriptionEditorModule } from '../type-viewer/description-editor/description-editor.module';
import { RouterModule } from '@angular/router';
import { LineageGraphModule } from '../type-viewer/lineage-graph/lineage-graph.module';
import { SchemaDiagramModule } from '../schema-diagram/schema-diagram.module';
import { AuthGuard } from 'src/app/services/auth.guard';
import { VynePrivileges } from 'src/app/services/user-info.service';
import { OperationViewContainerComponent } from 'src/app/operation-view/operation-view-container.component';

@NgModule({
  imports: [
    MatToolbarModule,
    SearchModule,
    CommonModule,
    DescriptionEditorModule,
    RouterModule,
    LineageGraphModule,
    SchemaDiagramModule,
    RouterModule.forChild([
      {
        path: ':serviceName',
        component: ServiceViewContainerComponent,
        canActivate: [AuthGuard],
        data: { requiredAuthority: VynePrivileges.BrowseCatalog }
      },
      {
        path: ':serviceName/:operationName',
        component: OperationViewContainerComponent,
        canActivate: [AuthGuard],
        data: { requiredAuthority: VynePrivileges.BrowseCatalog }
      },
    ])
  ],
  exports: [ServiceViewContainerComponent, ServiceViewComponent],
  declarations: [ServiceViewComponent, ServiceViewContainerComponent],
  providers: [],
})
export class ServiceViewModule {
}
