import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SchemaDiagramComponent } from './schema-diagram/schema-diagram.component';



@NgModule({
  declarations: [
    SchemaDiagramComponent
  ],
  imports: [
    CommonModule
  ],
  exports: [
    SchemaDiagramComponent
  ]
})
export class SchemaDiagramModule { }
