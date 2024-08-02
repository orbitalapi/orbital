import {Route} from "@angular/router";
import {PolicyManagerComponent} from "./policy-manager.component";
import {PolicyManagerWizardComponent} from "./policy-manager-wizard/policy-manager-wizard.component";
import {PolicyEditorComponent} from "./policy-editor/policy-editor.component";

export const policyManagerRoutes: Route[] = [
  {
    path: '',
    component: PolicyManagerComponent,
  },
  {
    path: 'get-started',
    component: PolicyManagerWizardComponent
  },
  {
    path: 'editor',
    component: PolicyEditorComponent
  },
]
