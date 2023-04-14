import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {QueryPanelModule} from './query-panel.module';
import {AngularSplitModule} from 'angular-split';
import {RouterTestingModule} from '@angular/router/testing';
import {ExpandingPanelSetModule} from '../expanding-panelset/expanding-panel-set.module';
import {TuiRootModule} from "@taiga-ui/core";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";

storiesOf('Query panel', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule,BrowserAnimationsModule,TuiRootModule, QueryPanelModule, ExpandingPanelSetModule, AngularSplitModule.forChild(), RouterTestingModule]
    })
  ).add('Query editor', () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 250px" >
    <query-editor></query-editor>
    </div>`,
    props: {}
  };
})
  .add('Bottom bar states', () => {
    return {
      template: `<div style="padding: 40px; width: 80%; height: 250px" >
      <app-panel-header title="Code">
        <app-query-editor-bottom-bar currentState="Editing"></app-query-editor-bottom-bar>
      </app-panel-header>
       <app-panel-header title="Code">
       <app-query-editor-bottom-bar currentState="Running" [queryStarted]="queryStartDate"></app-query-editor-bottom-bar>
      </app-panel-header>
       <app-panel-header title="Code">
        <app-query-editor-bottom-bar currentState="Running" [queryStarted]="aMinuteAgo"></app-query-editor-bottom-bar>
      </app-panel-header>
       <app-panel-header title="Code">
       <app-query-editor-bottom-bar currentState="Error" error="A query failed to execute."></app-query-editor-bottom-bar>
      </app-panel-header>
    </div>`,
      props: {
        queryStartDate: new Date(),
        aMinuteAgo: new Date(new Date().getTime() - (1000 * 60))
      }
    };
  })
  .add('save panel', () => {
    return {
      template: `
<tui-root>
<div style="padding: 40px; " >
<app-save-query-panel [packages]="projects"></app-save-query-panel>
</div>
</tui-root>
      `,
      props: {
        projects: [{
          "identifier": {
            "organisation": "io.vyne",
            "name": "my-project",
            "version": "1.0.0",
            "unversionedId": "io.vyne/core-types",
            "id": "io.vyne/core-types/1.0.0",
            "uriSafeId": "io.vyne:core-types:1.0.0"
          },
          "health": {"status": "Healthy", "message": null, "timestamp": "2023-04-14T06:54:00.040411019Z"},
          "sourceCount": 11,
          "warningCount": 0,
          "errorCount": 0,
          "publisherType": "Pushed",
          "editable": true,
          "packageConfig": null,
          "uriPath": "io.vyne:core-types:1.0.0"
        }]
      }
    }
  })
;
