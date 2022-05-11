import {NgModule} from '@angular/core';
import {VyneComponent} from './vyne.component';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatToolbarModule} from '@angular/material/toolbar';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule, DatePipe} from '@angular/common';
import {MatListModule} from '@angular/material/list';
import {RouterModule} from '@angular/router';
import {MatSnackBarModule} from '@angular/material/snack-bar';
import {SystemAlertComponent} from '../system-alert/system-alert.component';
import {SystemAlertModule} from '../system-alert/system-alert.module';
import {TuiRootModule} from '@taiga-ui/core';

@NgModule({
    imports: [
        MatSidenavModule,
        MatToolbarModule,
        BrowserModule,
        CommonModule,
        MatListModule,
        RouterModule,
        MatSnackBarModule,
        SystemAlertModule,
        TuiRootModule
    ],
  exports: [VyneComponent],
  declarations: [VyneComponent],
  providers: [DatePipe],
})
export class VyneModule {
}
