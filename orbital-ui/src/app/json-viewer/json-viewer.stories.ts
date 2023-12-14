import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import { JsonViewerModule } from 'src/app/json-viewer/json-viewer.module';

storiesOf('Json Viewer', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, JsonViewerModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 40px; height: 800px;">
<app-json-viewer style="height: 100%;" [json]="json"></app-json-viewer>
    </div>`,
      props: {
        json: `{
  "employees": [
    {
      "id": 1,
      "name": "John Doe",
      "age": 32,
      "position": "Manager"
    },
    {
      "id": 2,
      "name": "Jane Doe",
      "age": 28,
      "position": "Developer"
    }
  ],
  "company": {
    "name": "ACME Inc.",
    "year_founded": 1990,
    "location": "USA"
  },
  "foo" : [ null ]
}`

      }
    };
  });
