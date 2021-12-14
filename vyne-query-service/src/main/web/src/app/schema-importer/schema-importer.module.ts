import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SchemaImporterComponent} from './schema-importer.component';
import {TuiAccordionModule, TuiBadgeModule, TuiTreeModule} from '@taiga-ui/kit';
import { SchemaMemberListComponent } from './schema-member-list.component';


@NgModule({
  exports: [SchemaImporterComponent],
  declarations: [SchemaImporterComponent, SchemaMemberListComponent],
  imports: [
    CommonModule,
    TuiTreeModule,
    TuiAccordionModule,
    TuiBadgeModule
  ]
})
export class SchemaImporterModule {
}
