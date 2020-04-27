import {ModuleWithProviders, NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TaxiEditorComponent} from './taxi-editor.component';
import {MONACO_EDITOR_CONFIG, MonacoEditorConfig} from './config';
import {MonacoEditorComponent} from './monaco-editor.component';

@NgModule({
  declarations: [TaxiEditorComponent, MonacoEditorComponent],
  imports: [
    CommonModule,
  ],
  exports: [TaxiEditorComponent]
})
export class TaxiEditorModule {
  public static forRoot(config: MonacoEditorConfig = {}): ModuleWithProviders<TaxiEditorModule> {
    return {
      ngModule: TaxiEditorModule,
      providers: [
        {provide: MONACO_EDITOR_CONFIG, useValue: config}
      ]
    };
  }
}
