import { UiCustomisations } from '../../environments/ui-customisations';
import { AuthGuard } from '../services/auth.guard';
import { VynePrivileges } from '../services/user-info.service';
import { EndpointListComponent } from './endpoint-list.component';
import { EndpointMonitorContainerComponent } from './endpoint-monitor-container.component';

export const endpointManagerRoutes = [
  {
    path: '',
    component: EndpointListComponent,
    title: `${UiCustomisations.productName}: Endpoints`,
    canActivate: [AuthGuard],
    data: {requiredAuthority: VynePrivileges.ViewPipelines},
  },
  {
    path: ':endpointName',
    component: EndpointMonitorContainerComponent,
    title: `${UiCustomisations.productName}: Endpoints`,
    canActivate: [AuthGuard],
    data: {requiredAuthority: VynePrivileges.EditPipelines},
  }
]
