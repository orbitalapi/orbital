import {moduleMetadata, storiesOf} from '@storybook/angular';
import {RouterTestingModule} from '@angular/router/testing';
import {TypedInstancePanelModule} from './typed-instance-panel.module';
import {sampleOrderEventType} from '../data-explorer/sample-type';


storiesOf('Typed instance panel', module)
  .addDecorator(
    moduleMetadata({
      imports: [TypedInstancePanelModule, RouterTestingModule],
    })
  )
  .add('typed instance panel', () => {
    return {
      template: `<div style="margin: 20px">
        <app-typed-instance-panel [instance]="value" [type]="testType"></app-typed-instance-panel>
    </div>`,
      props: {
        testType: sampleOrderEventType,
        value: {
          'typeName': 'bank.orders.OrderEventType',
          'value': 'Open'
        }
      }
    };
  });


