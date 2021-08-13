import {moduleMetadata, storiesOf} from '@storybook/angular';
import {ObjectViewComponent} from '../../object-view/object-view.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {PipelinesModule} from '../pipelines.module';
import {TypeAutocompleteModule} from '../../type-autocomplete/type-autocomplete.module';
import {testSchema} from '../../object-view/test-schema';
import {prepareSchema} from '../../services/types.service';


storiesOf('Pipeline Builder', module)
  .addDecorator(
    moduleMetadata({
      imports: [CommonModule, BrowserModule, PipelinesModule, TypeAutocompleteModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px">
    <app-pipeline-builder [schema]="schema"></app-pipeline-builder>
    </div>`,
    props: {
      schema: prepareSchema(testSchema as any)
    }
  };
});
