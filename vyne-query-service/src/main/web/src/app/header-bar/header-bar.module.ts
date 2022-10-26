import { NgModule } from '@angular/core';

import { HeaderBarComponent } from './header-bar.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { SearchModule } from '../search/search.module';
import { AvatarComponent } from './avatar.component';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule } from '@angular/material/dialog';
import { ConfirmationDialogModule } from 'src/app/confirmation-dialog/confirmation-dialog.module';

@NgModule({
  imports: [
    MatToolbarModule,
    SearchModule,
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDialogModule,
    ConfirmationDialogModule
  ],
  exports: [HeaderBarComponent, AvatarComponent],
  declarations: [HeaderBarComponent, AvatarComponent],
  providers: [],
})
export class HeaderBarModule {
}
