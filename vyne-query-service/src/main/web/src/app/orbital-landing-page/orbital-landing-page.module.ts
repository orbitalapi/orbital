import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrbitalLandingPageComponent } from './orbital-landing-page.component';
import { RouterModule } from '@angular/router';
import { SchemaDiagramModule } from 'src/app/schema-diagram/schema-diagram.module';
import { OrbitalLandingPageContainerComponent } from './orbital-landing-page-container.component';
import { TuiButtonModule } from '@taiga-ui/core';


@NgModule({
  declarations: [
    OrbitalLandingPageComponent,
    OrbitalLandingPageContainerComponent
  ],
  imports: [
    CommonModule,
    SchemaDiagramModule,
    TuiButtonModule,
    RouterModule,
  ]
})
export class OrbitalLandingPageModule {
}