import {Route} from "@angular/router";
import {AuthGuard} from '../services/auth.guard';
import {VynePrivileges} from '../services/user-info.service';
import {PolicyManagerComponent} from "./policy-manager.component";
import {PolicyManagerWizardComponent} from "./policy-manager-wizard/policy-manager-wizard.component";
import {PolicyEditorComponent} from "./policy-editor/policy-editor.component";

export const policyManagerRoutes: Route[] = [
  {
    path: '',
    component: PolicyManagerComponent,
    canActivate: [AuthGuard],
    data: {requiredAuthority: VynePrivileges.BrowseSchema},
  },
  {
    path: 'get-started',
    component: PolicyManagerWizardComponent,
    canActivate: [AuthGuard],
    data: {requiredAuthority: VynePrivileges.EditSchema},
  },
  {
    path: 'editor',
    redirectTo: 'editor/',
    pathMatch: 'full'
  },
  {
    path: 'editor/:policyName',
    component: PolicyEditorComponent,
    canActivate: [AuthGuard],
    data: {requiredAuthority: VynePrivileges.EditSchema},
  }
]
