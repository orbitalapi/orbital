import { Route } from '@angular/router';
import { DataSourceImportComponent } from '../data-source-import/data-source-import.component';
import { OperationViewContainerComponent } from '../operation-view/operation-view-container.component';
import { ServiceViewContainerComponent } from '../service-view/service-view-container.component';
import { AuthGuard } from '../services/auth.guard';
import { VynePrivileges } from '../services/user-info.service';
import { DataSourceDetailViewComponent } from './data-source-detail-view.component';
import { DataSourceManagerHeaderComponent }from './data-source-manager-header.component';
import { DataSourceManagerComponent } from './data-source-manager.component';

export const dataSourceManagerRoutes: Route[] = [
  // NOTE: the ordering of the 'add' route being before the '' route matters
  {
    path: 'add',
    component: DataSourceManagerHeaderComponent,
    canActivate: [AuthGuard],
    data: {requiredAllAuthorities: [VynePrivileges.EditConnections, VynePrivileges.EditSchema]},
    children: [
      {
        path: '',
        component: DataSourceImportComponent,
        children: [
          {
            path: 'configure',
            component: DataSourceImportComponent,
          }
        ]
      },
    ]
  },
  {
    path: '',
    component: DataSourceManagerHeaderComponent,
    canActivate: [AuthGuard],
    data: {requiredAuthority: VynePrivileges.ViewConnections},
    children: [
      {
        path: '',
        component: DataSourceManagerComponent,
        children: [
          {
            path: 'services/:serviceName',
            component: ServiceViewContainerComponent,
            canActivate: [AuthGuard],
            data: { requiredAuthority: VynePrivileges.BrowseCatalog },
          },
          {
            path: 'services/:serviceName/:operationName',
            component: OperationViewContainerComponent,
            canActivate: [AuthGuard],
            data: { requiredAuthority: VynePrivileges.BrowseCatalog },
          },
          {
            path: ':packageUri/:connectionName',
            component: DataSourceDetailViewComponent,
            canActivate: [AuthGuard],
            data: {requiredAuthority: VynePrivileges.ViewConnections},
          },
        ]
      },
    ],
  },
  {
    path: 'data-source', // routing from the data-source-import component
    redirectTo: 'add'
  },

  // Think this may have been used for editing the DB connections...
  // Commenting out for now (see the rebuildForm() comment in connection-editor.component)
  /*{
    path: 'jdbc/:connectionName',
    component: DbConnectionWizardComponent,
    canActivate: [AuthGuard],
    data: {requiredAuthority: VynePrivileges.EditConnections}
  }*/
]
