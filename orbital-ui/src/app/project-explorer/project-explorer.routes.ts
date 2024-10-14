import {UiCustomisations} from '../../environments/ui-customisations';
import {ProjectImportComponent} from '../project-import/project-import.component';
import {AuthGuard} from '../services/auth.guard';
import {VynePrivileges} from '../services/user-info.service';
import {ProjectExplorerComponent} from './project-explorer.component';
import {Route} from '@angular/router';
import {ProjectExplorerContainerComponent} from './project-explorer-container.component';
import {ProjectSummaryViewComponent} from './project-summary-view.component';
import {ProjectErrorListComponent} from './project-error-list.component';

export const projectExplorerRoutes: Route[] = [
  {
    path: 'project-import',
    component: ProjectImportComponent,
    canActivate: [AuthGuard],
    data: {requiredAuthority: VynePrivileges.EditSchema},
  },
  {
    path: '',
    component: ProjectExplorerContainerComponent,
    children: [
      {
        path: '',
        component: ProjectSummaryViewComponent,
        title: `${UiCustomisations.productName}: Projects`
      },
      {
        path: 'problems',
        component: ProjectErrorListComponent,
        title: `${UiCustomisations.productName}: Projects`
      },
      {
        path: ':packageName',
        component: ProjectExplorerComponent,
        title: `${UiCustomisations.productName}: Projects`
      },
      {
        path: ':packageName/:selectedTab',
        component: ProjectExplorerComponent,
        title: `${UiCustomisations.productName}: Projects`
      },
      {
        path: ':packageName/:selectedTab/**',
        component: ProjectExplorerComponent,
        title: `${UiCustomisations.productName}: Projects`
      },
    ],
  },
]

