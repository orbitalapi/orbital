import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SchemaDiagramComponent } from './schema-diagram.component';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { MatButtonModule } from '@angular/material/button';
import {SchemaMultiSelectComponent} from './schema-multi-select.component';

@NgModule({
  declarations: [
    SchemaDiagramComponent,
  ],
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    SchemaMultiSelectComponent
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
