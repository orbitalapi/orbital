import {moduleMetadata, storiesOf} from '@storybook/angular';
import {FilterBarModule} from './filter-bar.module';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {testSchema} from '../object-view/test-schema';
import {displayTypeName, filterTypeByName} from './type-filters';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

storiesOf('Filter bar', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [FilterBarModule, CommonModule, BrowserModule, BrowserAnimationsModule]
    })
  ).add('default', () => {
  return {
    template: `<div style="padding: 40px">
<app-filter-bar [subjects]="subjects"
[subjectDisplayWith]="subjectDisplayAs"
[subjectFilter]="subjectFilter"
></app-filter-bar>
</div>`,
    props: {
      subjects: testSchema.types,
      subjectDisplayAs: displayTypeName,
      subjectFilter: filterTypeByName
    }
  };
});
