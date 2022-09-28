import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FullscreenToggleComponent } from './fullscreen-toggle.component';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';


@NgModule({
  declarations: [
    FullscreenToggleComponent
  ],
  exports: [
    FullscreenToggleComponent
  ],
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule
  ]
})
export class FullscreenToggleModule {

  constructor(private matIconRegistry: MatIconRegistry, private domSanitizer: DomSanitizer) {
    this.matIconRegistry.addSvgIcon(
      `fullscreen-on`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/full-screen-on.svg`)
    );

    this.matIconRegistry.addSvgIcon(
      `fullscreen-off`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/full-screen-off.svg`)
    );
  }
}
