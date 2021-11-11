import {moduleMetadata, storiesOf} from '@storybook/angular';
import {testSchema} from '../object-view/test-schema';
import {TypedEditorModule} from './type-editor.module';

storiesOf('Type Editor', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [TypedEditorModule]
    })
  ).add('Default', () => {
  return {
    template: `<div style="padding: 40px; width: 60rem;">
<app-type-editor [schema]="schema"></app-type-editor>
</div>`,
    props: {
      schema: testSchema
    }
  };
}).add('Card', () => {
  return {
    template: `<div style="padding: 40px; width: 60rem;">
<app-type-editor-card [schema]="schema"></app-type-editor-card>
</div>`,
    props: {
      schema: testSchema
    }
  };
}).add('Popup', () => {
  return {
    template: `<div style="padding: 40px; width: 60rem;">
<app-type-editor-popup [schema]="schema"></app-type-editor-popup>
</div>`,
    props: {
      schema: testSchema
    }
  };
})

