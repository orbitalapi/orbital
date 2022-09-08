import { NgModule } from '@angular/core';
import { TaxiPlaygroundAppComponent } from './taxi-playground-app.component';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { TaxiPlaygroundModule } from 'src/app/taxi-playground/taxi-playground.module';
import { AngularSplitModule } from 'angular-split';
import { CodeEditorModule } from 'src/app/code-editor/code-editor.module';
import { SchemaDiagramModule } from 'src/app/schema-diagram/schema-diagram.module';
import { HttpClientModule } from '@angular/common/http';
import { AuthModule } from '@auth0/auth0-angular';


@NgModule({
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    TaxiPlaygroundModule,
    AngularSplitModule,
    CodeEditorModule,
    SchemaDiagramModule,
    HttpClientModule,
    AuthModule.forRoot({
      domain: 'orbitalhq.eu.auth0.com',
      clientId: 'ZaDGRQWEfgTFtlWVR9AXWg9vOiBxgVPv'
    })
  ],
  declarations: [TaxiPlaygroundAppComponent],
  exports: [TaxiPlaygroundAppComponent],
  providers: [],
  bootstrap: [TaxiPlaygroundAppComponent]
})
export class TaxiPlaygroundAppModule {
}
