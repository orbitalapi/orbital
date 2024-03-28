import { NgModule } from '@angular/core';
import { TuiProgressModule } from '@taiga-ui/kit';
import { VyneComponent } from './vyne.component';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { CommonModule, DatePipe } from '@angular/common';
import { MatLegacyListModule as MatListModule } from '@angular/material/legacy-list';
import { RouterModule } from '@angular/router';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { SystemAlertModule } from '../system-alert/system-alert.module';
import { TUI_ALERT_POSITION, TuiButtonModule, TuiSvgModule } from '@taiga-ui/core';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatIconModule } from '@angular/material/icon';
import { DraftManagementBarModule } from "../draft-management-bar/draft-management-bar.module";
import { HeaderBarModule } from "../header-bar/header-bar.module";

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
        DraftManagementBarModule,
        HeaderBarModule,
        TuiSvgModule,
        TuiProgressModule
    ],
  exports: [VyneComponent],
  declarations: [VyneComponent],
  providers: [
    DatePipe,
    { provide: TUI_ALERT_POSITION, useValue: '2rem auto 0 auto' },
  ],
})
export class VyneModule {
}
