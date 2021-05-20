import {moduleMetadata, storiesOf} from '@storybook/angular';
import {DbConnectionEditorModule} from './db-connection-editor.module';

storiesOf('Db Connection Editor', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [DbConnectionEditorModule]
    })
  ).add('Connection editor', () => {
  return {
    template: `<div style="padding: 40px; width: 100%;">
<app-db-connection-editor></app-db-connection-editor>
</div>`
  };
});
