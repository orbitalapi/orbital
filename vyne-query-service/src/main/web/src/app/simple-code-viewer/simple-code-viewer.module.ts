import {CovalentHighlightModule} from '@covalent/highlight';
import {SimpleCodeViewerComponent} from './simple-code-viewer.component';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';

/**
 * Simple Code Viewer is more lightweight than the full
 * monaco editor used in the CodeViewerComponent.
 * However, we don't have taxi language support in this one.
 */
@NgModule({
  imports: [CovalentHighlightModule, BrowserModule, CommonModule],
  exports: [SimpleCodeViewerComponent],
  declarations: [SimpleCodeViewerComponent],
  providers: []
})
export class SimpleCodeViewerModule {
}
