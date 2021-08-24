import {moduleMetadata, storiesOf} from '@storybook/angular';
import {APP_BASE_HREF, CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {PipelinesModule} from '../pipelines.module';
import {TypeAutocompleteModule} from '../../type-autocomplete/type-autocomplete.module';
import {prepareSchema} from '../../services/types.service';
import {testSchema} from '../../object-view/test-schema';
import {pipeline} from './pipeline-sample';
import {HttpClientTestingModule} from '@angular/common/http/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {ConfirmationDialogComponent} from '../../confirmation-dialog/confirmation-dialog.component';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';

storiesOf('Pipeline view', module)
  .addDecorator(
    moduleMetadata({
      imports: [
        CommonModule,
        BrowserModule,
        PipelinesModule,
        TypeAutocompleteModule,
        HttpClientTestingModule,
        RouterTestingModule
      ],
      entryComponents: [ConfirmationDialogComponent],
      providers: [
        [{provide: MAT_DIALOG_DATA, useValue: {}}],
      ]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px">
    <app-pipeline-view [pipeline]="pipeline" [schema]="schema"></app-pipeline-view>
    </div>`,
    props: {
      pipeline: pipeline,
      schema: testSchema
    }
  };
});
