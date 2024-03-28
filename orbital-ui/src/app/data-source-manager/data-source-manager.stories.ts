import {moduleMetadata, storiesOf} from '@storybook/angular';
import {RouterTestingModule} from '@angular/router/testing';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {HttpClientTestingModule} from '@angular/common/http/testing';

storiesOf('Connection manager', module)
  .addDecorator(
    moduleMetadata({
      imports: [BrowserModule, BrowserAnimationsModule, HttpClientTestingModule, RouterTestingModule]
      ,
    })
  )
  .add('connection manager', () => {
    return {
      template: `
        <div style="margin: 20px">
          <app-data-source-manager></app-data-source-manager>
        </div>
      `
    };
  });
