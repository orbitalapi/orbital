import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SchemaDiagramComponent } from './schema-diagram/schema-diagram.component';
import { AngularResizeEventModule } from 'angular-resize-event';



@NgModule({
  declarations: [
    SchemaDiagramComponent
  ],
  imports: [
    CommonModule,
    AngularResizeEventModule
  ],
  exports: [
    SchemaDiagramComponent
  ]
})
export class SchemaDiagramModule { }
