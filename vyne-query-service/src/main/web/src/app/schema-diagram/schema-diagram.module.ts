import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SchemaDiagramComponent } from './schema-diagram/schema-diagram.component';
import { AngularResizeEventModule } from 'angular-resize-event';
import { FullscreenToggleModule } from '../fullscreen-toggle/fullscreen-toggle.module';



@NgModule({
  declarations: [
    SchemaDiagramComponent
  ],
    imports: [
        CommonModule,
        AngularResizeEventModule,
        FullscreenToggleModule
    ],
  exports: [
    SchemaDiagramComponent
  ]
})
export class SchemaDiagramModule { }
