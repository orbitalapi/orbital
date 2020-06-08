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

storiesOf('Data Explorer', module)
  .addDecorator(
    moduleMetadata({
      imports: [DataExplorerModule],
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
  .add('file icon', () => {
  return {
    template: `<div style="margin: 20px"><app-file-extension-icon extension="json"></app-file-extension-icon> </div>`,
    props: {}
  };
})

