import { NgModule } from '@angular/core';

import { HeaderBarComponent } from './header-bar.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { SearchModule } from '../search/search.module';
import { AvatarComponent } from './avatar.component';
import {CommonModule} from '@angular/common';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyMenuModule as MatMenuModule } from '@angular/material/legacy-menu';
import { MatLegacyDialogModule as MatDialogModule } from '@angular/material/legacy-dialog';
import { ConfirmationDialogModule } from 'src/app/confirmation-dialog/confirmation-dialog.module';
import {WorkspaceSelectorModule} from "../workspace-selector/workspace-selector.module";
import {TuiAvatarModule} from "@taiga-ui/kit";
import {TuiButtonModule, TuiDataListModule, TuiHostedDropdownModule} from "@taiga-ui/core";

@NgModule({
  imports: [
    MatToolbarModule,
    SearchModule,
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDialogModule,
    ConfirmationDialogModule,
    WorkspaceSelectorModule,
    TuiAvatarModule,
    TuiHostedDropdownModule,
    TuiDataListModule,
    TuiButtonModule,
  ],
  exports: [HeaderBarComponent, AvatarComponent],
  declarations: [HeaderBarComponent, AvatarComponent],
  providers: [],
})
export class HeaderBarModule {
}
