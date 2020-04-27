import {moduleMetadata, storiesOf} from '@storybook/angular';
import {TaxiEditorModule} from './taxi-editor.module';

storiesOf('Taxi Editor', module)
  .addDecorator(
    moduleMetadata({
      imports: [TaxiEditorModule.forRoot()]
    })
  ).add('default', () => {
  return {
    template: `<app-taxi-editor></app-taxi-editor>`
  };
});
