import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlaygroundToolbarComponent } from 'src/app/taxi-playground/toolbar/playground-toolbar.component';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { MatButtonModule } from '@angular/material/button';
import { TuiDialogModule } from '@taiga-ui/core';
import { ReactiveFormsModule } from '@angular/forms';
import { SubscribeDialogComponent } from './subscribe-dialog/subscribe-dialog.component';
import { TuiInputModule } from '@taiga-ui/kit';


@NgModule({
  declarations: [
    PlaygroundToolbarComponent,
    SubscribeDialogComponent
  ],
  exports: [
    PlaygroundToolbarComponent
  ],
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    TuiDialogModule,
    TuiInputModule,
    ReactiveFormsModule
  ],
  entryComponents: [
    SubscribeDialogComponent
  ]
})
export class TaxiPlaygroundModule {

  constructor(private matIconRegistry: MatIconRegistry, private domSanitizer: DomSanitizer) {
    this.matIconRegistry.addSvgIcon(
      `brandGitHub`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/brand-github.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `slack`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/slack.svg`)
    );
    this.matIconRegistry.addSvgIcon(
      `vyneDots`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/vyne-logo-dots.svg`)
    );
  }
}

