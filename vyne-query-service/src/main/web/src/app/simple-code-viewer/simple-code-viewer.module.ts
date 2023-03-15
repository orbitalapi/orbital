import {CovalentHighlightModule} from '@covalent/highlight';
import {SimpleCodeViewerComponent} from './simple-code-viewer.component';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {HighlightModule, HIGHLIGHT_OPTIONS} from 'ngx-highlightjs';
import {taxiLangDef} from '../code-viewer/taxi-lang-def';
import {of} from 'rxjs';

export function loadHighlightJs() {
  return  () => import('highlight.js');
}
export function loadTypescriptLanguage() {
  return () => import('highlight.js/lib/languages/typescript');
}
/**
 * Simple Code Viewer is more lightweight than the full
 * monaco editor used in the CodeViewerComponent.
 * However, we don't have taxi language support in this one.
 */
@NgModule({
  imports: [
    CommonModule,
    HighlightModule
  ],
  exports: [SimpleCodeViewerComponent],
  declarations: [SimpleCodeViewerComponent],
  providers: [
    {
      provide: HIGHLIGHT_OPTIONS,
      useValue: {
        fullLibraryLoader: loadHighlightJs(),
        languages: {
          typescript: loadTypescriptLanguage(),
        }
      }
    }
  ]
})
export class SimpleCodeViewerModule {
}
