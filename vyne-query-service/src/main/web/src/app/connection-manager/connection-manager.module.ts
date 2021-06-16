import {NgModule} from '@angular/core';

import {ConnectionManagerComponent} from './connection-manager.component';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {MatButtonModule} from '@angular/material/button';
import {MatMenuModule} from '@angular/material/menu';
import {RouterModule} from '@angular/router';
import {DbConnectionEditorModule} from '../db-connection-editor/db-connection-editor.module';
import {ConnectionListComponent} from './connection-list.component';
import {CommonModule} from '@angular/common';

@NgModule({
  imports: [
    HeaderBarModule,
    MatButtonModule,
    MatMenuModule,
    RouterModule,
    CommonModule,
    DbConnectionEditorModule
  ],
  exports: [ConnectionManagerComponent, ConnectionListComponent],
  declarations: [ConnectionManagerComponent, ConnectionListComponent],
  providers: [],
})
export class ConnectionManagerModule {
}
