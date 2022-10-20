import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrbitalShellComponent } from './orbital-shell.component';
import { RouterModule } from '@angular/router';
import { TUI_SANITIZER, TuiHintModule, TuiSvgModule } from '@taiga-ui/core';
import { NgDompurifySanitizer } from '@tinkoff/ng-dompurify';
import { TooltipModule } from 'src/app/tooltip/tooltip.module';
import { SidebarComponent } from 'src/app/orbital-shell/sidebar/sidebar.component';
import { NavbarComponent } from './navbar/navbar.component';
import { OrbitalLandingPageModule } from 'src/app/orbital-landing-page/orbital-landing-page.module';

@NgModule({
  declarations: [
    OrbitalShellComponent,
    SidebarComponent,
    NavbarComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    TuiSvgModule,
    TooltipModule,
    TuiHintModule,
    OrbitalLandingPageModule
  ],
  providers: [
    {
      provide: TUI_SANITIZER,
      useClass: NgDompurifySanitizer,
    },
  ],
})
export class OrbitalShellModule {
}
