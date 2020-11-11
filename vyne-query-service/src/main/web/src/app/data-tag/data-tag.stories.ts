import {moduleMetadata, storiesOf} from '@storybook/angular';
import {DataTagModule} from './data-tag.module';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';

storiesOf('Data tag', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [
        DataTagModule,
        CommonModule,
        BrowserModule
      ]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px">
    <app-data-tag [key]="data[0].key" [value]="data[0].value" [tooltip]="data[0].tooltip"></app-data-tag>
    <app-data-tag [key]="data[1].key" [value]="data[1].value" [tooltip]="data[1].tooltip"></app-data-tag>
    <app-data-tag [key]="data[2].key" [keyValuePairs]="data[2].attributes"></app-data-tag>
</div>`,
    props: {
      data: [
        {key: 'DataQuality', value: 'Low', tooltip: 'Hello, world'},
        {key: 'Canonical'},
        {
          key: 'DataWarning', attributes: {
            deprecated: true,
            replacedBy: 'Another thing'
          }
        }
      ]
    }
  }
})
