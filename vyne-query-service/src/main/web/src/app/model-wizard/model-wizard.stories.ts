import {moduleMetadata, storiesOf} from '@storybook/angular';
import {ObjectViewComponent} from '../object-view/object-view.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {ModelWizardModule} from './model-wizard.module';

storiesOf('Model wizard', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, ModelWizardModule]
    })
  ).add('Attribute panel', () => {
  return {
    template: `<div style="padding: 40px">
    <app-attribute-panel></app-attribute-panel>
   `,
    props: {}
  };
});
