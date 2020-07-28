import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { CodeEditorComponent } from './code-editor.component';


storiesOf('CodeEditor', module)
  .addDecorator(
    moduleMetadata({
      declarations: [CodeEditorComponent],
      imports: [CommonModule, BrowserModule,  MonacoEditorModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="height: 100vh"> <app-code-editor [editorOptions]="editorOptions"  > </app-code-editor> </div>`,
    props: {
      editorOptions: { theme: 'vs-dark', language: 'vyneQL' }
    }
  };
});

