import { UiCustomisations } from '../../environments/ui-customisations';
import { OperationViewContainerComponent } from '../operation-view/operation-view-container.component';
import { AuthGuard } from '../services/auth.guard';
import { VynePrivileges } from '../services/user-info.service';
import { ServiceViewContainerComponent } from './service-view-container.component';

export const serviceViewRoutes = [
  {
    path: ':serviceName',
    component: ServiceViewContainerComponent,
    canActivate: [AuthGuard],
    data: { requiredAuthority: VynePrivileges.BrowseSchema },
    title: `${UiCustomisations.productName}: Service`
  },
  {
    path: ':serviceName/:operationName',
    component: OperationViewContainerComponent,
    canActivate: [AuthGuard],
    data: { requiredAuthority: VynePrivileges.BrowseSchema },
    title: `${UiCustomisations.productName}: Service`
  },
]
