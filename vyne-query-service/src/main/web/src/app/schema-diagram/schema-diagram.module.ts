import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SchemaDiagramComponent } from './schema-diagram/schema-diagram.component';
import { AngularResizeEventModule } from 'angular-resize-event';
import { FullscreenToggleModule } from '../fullscreen-toggle/fullscreen-toggle.module';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { MatButtonModule } from '@angular/material/button';



@NgModule({
  declarations: [
    SchemaDiagramComponent
  ],
    imports: [
        CommonModule,
        AngularResizeEventModule,
        FullscreenToggleModule,
        MatIconModule,
        MatButtonModule,
    ],
  exports: [
    SchemaDiagramComponent
  ]
})
export class SchemaDiagramModule {

  constructor(private matIconRegistry: MatIconRegistry, private domSanitizer: DomSanitizer) {
    this.matIconRegistry.addSvgIcon(
      `download`,
      this.domSanitizer.bypassSecurityTrustResourceUrl(`../../assets/img/tabler/download.svg`)
    );
  }
}
