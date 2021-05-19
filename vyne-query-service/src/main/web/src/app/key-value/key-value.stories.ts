import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {KeyValueModule} from './key-value.module';

storiesOf('Key value', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, KeyValueModule]
    })
  ).add('Default', () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 250px; display: flex; background-color: #f5f6fa" >
        <app-key-value title="Cask name" value="bbg.referencedata.TraderResolution"></app-key-value>
        <app-key-value title="Created at" value="May 18, 2021, 3:38:17 PM"></app-key-value>
        <app-key-value title="Number of records" value="2"></app-key-value>
    </div>
 `,

  };
});
