import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {SimpleCodeViewerComponent} from './simple-code-viewer.component';
import {sampleTaxi} from './sample-taxi';
import {SimpleCodeViewerModule} from './simple-code-viewer.module';

storiesOf('Simple code viewer', module)
  .addDecorator(
    moduleMetadata({
      imports: [CommonModule, BrowserModule, SimpleCodeViewerModule]
    })
  ).add('default', () => {
  return {
    template: `<app-simple-code-viewer lang="taxi" [content]="source"></app-simple-code-viewer>`,
    props: {
      source: sampleTaxi
    }
  };
});

