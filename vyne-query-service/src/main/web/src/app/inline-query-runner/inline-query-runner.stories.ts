import {moduleMetadata, storiesOf} from '@storybook/angular';
import {InlineQueryRunnerModule} from './inline-query-runner.module';
import {VyneServicesModule} from '../services/vyne-services.module';

storiesOf('Inline Query Runner', module)
  .addDecorator(
    moduleMetadata({
      imports: [InlineQueryRunnerModule, VyneServicesModule]
    })
  ).add('data source toolbar', () => {
  return {
    template: `<div style="margin: 20px; width: 300px">
<app-inline-query-runner targetType="CustomerName"></app-inline-query-runner></div>`,
    props: {}
  };
});
