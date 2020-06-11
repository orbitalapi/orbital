import {NgModule} from '@angular/core';
import {VyneComponent} from './vyne.component';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatToolbarModule} from '@angular/material/toolbar';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {MatListModule} from '@angular/material/list';
import {RouterModule} from '@angular/router';

@NgModule({
  imports: [
    MatSidenavModule,
    MatToolbarModule,
    BrowserModule,
    CommonModule,
    MatListModule,
    RouterModule
  ],
  exports: [VyneComponent],
  declarations: [VyneComponent],
  providers: [],
})
export class VyneModule {
}
