import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {service} from '../service-view/service-schema';
import {OperationViewModule} from './operation-view.module';
import {RouterTestingModule} from '@angular/router/testing';

storiesOf('Operation view', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, OperationViewModule, RouterTestingModule]
    })
  ).add('Operation view', () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 100%" >
    <app-operation-view [operation]="operation"></app-operation-view>
    </div>`,
    props: {
      operation: service.operations[0]
    }
  };
});
