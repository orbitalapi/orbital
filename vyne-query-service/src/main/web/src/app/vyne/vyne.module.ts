import { NgModule } from '@angular/core';
import { VyneComponent } from './vyne.component';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { CommonModule, DatePipe } from '@angular/common';
import { MatListModule } from '@angular/material/list';
import { RouterModule } from '@angular/router';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { SystemAlertModule } from '../system-alert/system-alert.module';
import { TuiButtonModule, TuiRootModule } from '@taiga-ui/core';
import { DraftManagementBarComponent } from '../draft-management-bar/draft-management-bar.component';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import {PolymorpheusModule} from "@tinkoff/ng-polymorpheus";
import {DraftManagementBarModule} from "../draft-management-bar/draft-management-bar.module";

@NgModule({
  imports: [
    MatSidenavModule,
    MatToolbarModule,
    CommonModule,
    MatListModule,
    RouterModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatIconModule,
    SystemAlertModule,
    TuiButtonModule,
    DraftManagementBarModule
  ],
  exports: [VyneComponent],
  declarations: [VyneComponent],
  providers: [DatePipe],
})
export class VyneModule {
}
