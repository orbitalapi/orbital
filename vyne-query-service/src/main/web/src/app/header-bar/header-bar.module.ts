import {NgModule} from '@angular/core';

import {HeaderBarComponent} from './header-bar.component';
import {MatToolbarModule} from '@angular/material/toolbar';
import {SearchModule} from '../search/search.module';
import { AvatarComponent } from './avatar.component';
import {CommonModule} from '@angular/common';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatMenuModule} from '@angular/material/menu';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatDialogModule} from '@angular/material/dialog';

@NgModule({
  imports: [
    MatToolbarModule,
    SearchModule,
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    BrowserAnimationsModule,
    MatDialogModule
  ],
  exports: [HeaderBarComponent, AvatarComponent],
  declarations: [HeaderBarComponent, AvatarComponent],
  providers: [],
})
export class HeaderBarModule {
}
