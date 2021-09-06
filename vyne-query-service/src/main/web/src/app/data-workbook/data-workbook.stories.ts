import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {DataWorkbookModule} from './data-workbook.module';
import {ParsedCsvContent} from '../services/types.service';
import {RouterTestingModule} from '@angular/router/testing';
import {testSchema} from '../object-view/test-schema';

const csvContents = `symbol,quantity
GBPNZD,15000
AUDNZD,20000`;

const parsedCsv: ParsedCsvContent = {
  headers: ['symbol', 'quantity'],
  records: [
    ['GBPNZD', '15000'],
    ['AUDNZD', '25000']
  ]
};

storiesOf('Data Workbook editor', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, DataWorkbookModule, RouterTestingModule]
    })
  )
  .add('data source panel', () => {
    return {
      template: `<div style="padding: 40px; background-color: #f5f6fa" >
<app-data-source-panel></app-data-source-panel>
    </div>`,
      props: {}
    };
  })
  .add('data source panel with file selected', () => {
    return {
      template: `<div style="padding: 40px; background-color: #f5f6fa" >
<app-data-source-panel
    [fileDataSource]="file"
    [fileContents]="fileContents"
    [parsedCsvContent]="parsedCsvContent"
></app-data-source-panel>
    </div>`,
      props: {
        fileContents: csvContents,
        parsedCsvContent: parsedCsv,
        file: {
          relativePath: '/src/foo/bar.csv',
          fileEntry: null
        }
      }
    };
  })
  .add('schema selector', () => {
    return {
      template: `<div style="padding: 40px; background-color: #f5f6fa" >
<app-workbook-schema-selector [schema]="schema"></app-workbook-schema-selector>
    </div>`,
      props: {
        fileContents: csvContents,
        parsedCsvContent: parsedCsv,
        schema: testSchema,
        file: {
          relativePath: '/src/foo/bar.csv',
          fileEntry: null
        }
      }
    };
  })
;
