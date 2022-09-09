import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import { CodeEditorComponent } from './code-editor.component';


storiesOf('CodeEditor', module)
  .addDecorator(
    moduleMetadata({
      declarations: [CodeEditorComponent],
      imports: [CommonModule, BrowserModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="height: 100vh"> <app-code-editor></app-code-editor> </div>`,
    props: {
    }
  };
});

