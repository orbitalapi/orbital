import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CaskViewerModule} from '../cask-viewer/cask-viewer.module';
import {SearchModule} from '../search/search.module';
import {RouterTestingModule} from '@angular/router/testing';
import {ConnectionManagerModule} from './connection-manager.module';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {HttpClientTestingModule} from '@angular/common/http/testing';

storiesOf('Connection manager', module)
  .addDecorator(
    moduleMetadata({
      imports: [ConnectionManagerModule, BrowserModule, BrowserAnimationsModule, HttpClientTestingModule, RouterTestingModule]
      ,
    })
  )
  .add('connection manager', () => {
    return {
      template: `
        <div style="margin: 20px">
          <app-connection-manager></app-connection-manager>
        </div>
      `
    };
  });
