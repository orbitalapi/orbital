import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import { TypeListModule } from 'src/app/type-list/type-list.module';
import { RouterTestingModule } from '@angular/router/testing';

storiesOf('Type catalog filter', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, TypeListModule, RouterTestingModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 40px">
<app-filter-types></app-filter-types>
    </div>`,
      props: {}
    };
  });
