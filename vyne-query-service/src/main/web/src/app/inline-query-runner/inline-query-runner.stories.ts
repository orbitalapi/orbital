import {moduleMetadata, storiesOf} from '@storybook/angular';
import {DataExplorerModule} from '../data-explorer/data-explorer.module';
import {RouterTestingModule} from '@angular/router/testing';
import {InlineResourcesMetadataTransformer} from '@angular/compiler-cli/src/transformers/inline_resources';
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
