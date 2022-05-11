import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SchemaDisplayTableComponent} from './schema-display-table.component';


@NgModule({
  declarations: [SchemaDisplayTableComponent],
  imports: [
    CommonModule
  ],
  exports: [SchemaDisplayTableComponent]
})
export class SchemaDisplayTableModule {
}
