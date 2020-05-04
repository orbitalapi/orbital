import {moduleMetadata, storiesOf} from '@storybook/angular';
import {ObjectViewComponent} from '../../object-view/object-view.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {PipelinesModule} from '../pipelines.module';
import {PipelineWizardComponent} from './pipeline-wizard.component';
import {TypeAutocompleteModule} from '../../type-autocomplete/type-autocomplete.module';
import {testSchema} from '../../object-view/test-schema';


storiesOf('Pipeline Wizard', module)
  .addDecorator(
    moduleMetadata({
      imports: [CommonModule, BrowserModule, PipelinesModule, TypeAutocompleteModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px">
    <app-pipeline-wizard [schema]="schema"></app-pipeline-wizard>
    </div>`,
    props: {
      schema: testSchema
    }
  };
});
