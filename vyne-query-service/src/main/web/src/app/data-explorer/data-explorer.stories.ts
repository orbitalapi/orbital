import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatTabsModule} from '@angular/material/tabs';
import {FormsModule} from '@angular/forms';
import {TypeViewerComponent} from '../type-viewer/type-viewer.component';
import {DataSourceUploadComponent} from './data-source-upload.component';
import {DataExplorerComponent} from './data-explorer.component';
import {TypeAutocompleteComponent} from '../type-autocomplete/type-autocomplete.component';
import {DataSourceToolbarComponent} from './data-source-toolbar.component';
import {MatInputModule} from '@angular/material/input';
import {MatIconModule} from '@angular/material/icon';
import {TypeAutocompleteModule} from '../type-autocomplete/type-autocomplete.module';
import {DataExplorerModule} from './data-explorer.module';
import {UploadFile} from 'ngx-file-drop';
import {sampleOrderEventType} from './sample-type';
import {RouterTestingModule} from '@angular/router/testing';
import {CsvOptions} from '../services/types.service';


storiesOf('Data Explorer', module)
  .addDecorator(
    moduleMetadata({
      imports: [DataExplorerModule, RouterTestingModule],
    })
  ).add('data source toolbar', () => {
  return {
    template: `<div style="margin: 20px"><app-data-source-toolbar></app-data-source-toolbar></div>`,
    props: {}
  };
})
  .add('csv selected', () => {
    return {
      template: `<div style="margin: 20px"><app-data-source-toolbar [fileDataSource]="dataSource"></app-data-source-toolbar></div>`,
      props: {
        dataSource: {
          relativePath: 'some-content.csv',
          fileEntry: {}
        } as UploadFile
      }
    };
  })
  .add('csv viewer', () => {
    return {
      template: `<div style="margin: 20px"><app-csv-viewer [source]="data" [firstRowAsHeaders]="true"></app-csv-viewer></div>`,
      props: {
        data: [
          ['Column A', 'Column B', 'Column C'],
          ['The quick', 'brown fox', 'jumps over'],
          ['the lazy', 'but very cute', 'pupppppppy!']
        ]
      }
    };
  })
  .add('file icon', () => {
    return {
      template: `<div style="margin: 20px"><app-file-extension-icon extension="json"></app-file-extension-icon> </div>`,
      props: {}
    };
  })
  .add('cask panel', () => {
    return {
      template: `<div style="margin: 20px">
       <app-cask-panel format="json" targetTypeName="demo.Customer"></app-cask-panel>
       <app-cask-panel format="csv" targetTypeName="demo.Customer" [csvOptions]="csvOptions"></app-cask-panel>
    </div>`,
      props: {
        csvOptions: new CsvOptions(true, ';', 'NULL')
      }
    };
  })

;

