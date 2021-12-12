import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SchemaImporterComponent} from './schema-importer.component';
import {TuiAccordionModule, TuiTreeModule} from '@taiga-ui/kit';


@NgModule({
  exports: [SchemaImporterComponent],
  declarations: [SchemaImporterComponent],
  imports: [
    CommonModule,
    TuiTreeModule,
    TuiAccordionModule
  ]
})
export class SchemaImporterModule {
}
