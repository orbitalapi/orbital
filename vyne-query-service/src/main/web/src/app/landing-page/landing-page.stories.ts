import {moduleMetadata, storiesOf} from '@storybook/angular';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {LandingPageModule} from './landing-page.module';
import {DATA_SOURCES, LandingPageCardConfig, RECENT_QUERIES} from './landing-page.component';
import {RouterTestingModule} from '@angular/router/testing';

storiesOf('Landing page', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, LandingPageModule, RouterTestingModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 100px; background-color: #f5f6fa;">
<app-landing-page></app-landing-page>
    </div>`,
      props: {}
    };
  }).add('card', () => {
    return {
      template: `<div style="padding: 100px; background-color: #f5f6fa; display: flex;">
<app-landing-card [isEmpty]="true" [cardConfig]="recentQueries"></app-landing-card>
<app-landing-card [isEmpty]="true" [cardConfig]="dataSources"></app-landing-card>
    </div>`,
      props: {
        recentQueries: RECENT_QUERIES,
        dataSources: DATA_SOURCES

      }
    };
  });
