import { ModuleWithProviders} from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { UsersComponent } from './users.component';
import { UsersFormComponent } from './form/form.component';

const routes: Routes = [{
    path: 'users',
    children: [{
      path: '',
      component: UsersComponent,
    }, {
      path: 'add',
      component: UsersFormComponent,
    }, {
      path: ':id/edit',
      component: UsersFormComponent,
    }],
}];

export const userRoutes: ModuleWithProviders = RouterModule.forChild(routes);
