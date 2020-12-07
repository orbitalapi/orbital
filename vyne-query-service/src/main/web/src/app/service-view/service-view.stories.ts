import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {QueryPanelModule} from '../query-panel/query-panel.module';
import {AngularSplitModule} from 'angular-split';
import {ServiceViewModule} from './service-view.module';
import {service} from './service-schema';

storiesOf('Service view', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, ServiceViewModule]
    })
  ).add('Service view', () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 100%" >
    <app-service-view [service]="service"></app-service-view>
    </div>`,
    props: {
      service: service
    }
  };
});
