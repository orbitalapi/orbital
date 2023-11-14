import {NgModule} from '@angular/core';

import {ConnectionManagerComponent} from './connection-manager.component';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {RouterModule} from '@angular/router';
import {DbConnectionEditorModule} from '../db-connection-editor/db-connection-editor.module';
import {ConnectionListComponent} from './connection-list.component';
import {CommonModule} from '@angular/common';
import {TuiButtonModule, TuiHintModule, TuiLinkModule} from '@taiga-ui/core';
import {AuthGuard} from 'src/app/services/auth.guard';
import {VynePrivileges} from 'src/app/services/user-info.service';
import {DbConnectionWizardComponent} from 'src/app/db-connection-editor/db-connection-wizard.component';
import {HeaderComponentLayoutModule} from 'src/app/header-component-layout/header-component-layout.module';
import {ConnectionDetailViewComponent} from './connection-detail-view.component';
import {ConnectionStatusComponent} from "./connection-status.component";
import {MomentModule} from "ngx-moment";

@NgModule({
  imports: [
    HeaderBarModule,
    MatButtonModule,
    MatMenuModule,
    RouterModule,
    CommonModule,
    DbConnectionEditorModule,
    TuiLinkModule,
    TuiButtonModule,
    RouterModule.forChild([
      {
        path: '',
        component: ConnectionManagerComponent,
        children: [
          {
            path: '',
            component: ConnectionListComponent,
            canActivate: [AuthGuard],
            data: {requiredAuthority: VynePrivileges.ViewConnections}
          },
          {
            path: 'new',
            component: DbConnectionWizardComponent,
            canActivate: [AuthGuard],
            data: {requiredAuthority: VynePrivileges.EditConnections}
          },
          {
            path: 'jdbc/:connectionName',
            component: DbConnectionWizardComponent,
            canActivate: [AuthGuard],
            data: {requiredAuthority: VynePrivileges.EditConnections}
          },
          {
            path: ':packageUri/:connectionName',
            component: ConnectionDetailViewComponent,
            canActivate: [AuthGuard],
            data: {requiredAuthority: VynePrivileges.ViewConnections}
          },

        ]
      },
    ]),
    HeaderComponentLayoutModule,
    TuiHintModule,
    MomentModule
  ],
  exports: [ConnectionManagerComponent, ConnectionListComponent],
  declarations: [ConnectionManagerComponent, ConnectionListComponent, ConnectionDetailViewComponent, ConnectionStatusComponent],
  providers: [],
})
export class ConnectionManagerModule {
}
