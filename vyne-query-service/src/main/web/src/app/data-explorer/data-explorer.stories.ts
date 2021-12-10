import {moduleMetadata, storiesOf} from '@storybook/angular';
import {DataExplorerModule} from './data-explorer.module';
import {RouterTestingModule} from '@angular/router/testing';
import {CsvOptions} from '../services/types.service';
import {NgxFileDropEntry} from 'ngx-file-drop';


storiesOf('Data Explorer', module)
  .addDecorator(
    moduleMetadata({
      imports: [DataExplorerModule, RouterTestingModule],
    })
  ).add('data source toolbar', () => ({
    template: `<div style="margin: 20px"><app-data-source-toolbar></app-data-source-toolbar></div>`,
    props: {}
  }))
  .add('csv selected', () => ({
      template: `<div style="margin: 20px"><app-data-source-toolbar [fileDataSource]="dataSource"></app-data-source-toolbar></div>`,
      props: {
        dataSource: {
          relativePath: 'some-content.csv',
          fileEntry: {}
        } as NgxFileDropEntry
      }
    }))
  .add('csv viewer', () => ({
      template: `<div style="margin: 20px"><app-csv-viewer [source]="data" [firstRowAsHeaders]="true"></app-csv-viewer></div>`,
      props: {
        data: {
          records: [
            ['The quick', 'brown fox', 'jumps over'],
            ['the lazy', 'but very cute', 'pupppppppy!']
          ],
          headers: ['Col 1', 'Col 2', 'Col 3']
        }
      }
    }))
  .add('file icon', () => ({
      template: `<div style="margin: 20px"><app-file-extension-icon extension="json"></app-file-extension-icon> </div>`,
      props: {}
    }))
  .add('cask panel', () => ({
      template: `<div style="margin: 20px">
       <app-cask-panel format="json" targetTypeName="demo.Customer"></app-cask-panel>
       <app-cask-panel format="csv" targetTypeName="demo.Customer" [csvOptions]="csvOptions"
       [xmlIngestionParameters]="xmlIngestionParameters"></app-cask-panel>
    </div>`,
      props: {
        csvOptions: new CsvOptions(true, ';', 'NULL')
      }
    }))

;

