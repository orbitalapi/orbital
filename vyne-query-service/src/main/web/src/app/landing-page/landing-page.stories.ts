import { moduleMetadata, storiesOf } from '@storybook/angular';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { LandingPageModule } from './landing-page.module';
import { DATA_SOURCES, RECENT_QUERIES } from './landing-page.component';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FILMS_SCHEMA } from '../schema-diagram/films-schema';
import { CHANGELOG_DATA } from '../changelog/changelog-data';

storiesOf('Landing page', module)
  .addDecorator(
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, BrowserAnimationsModule, LandingPageModule, RouterTestingModule, HttpClientTestingModule]
    })
  )
  .add('default', () => {
    return {
      template: `<div style="padding: 20px; background-color: #f5f6fa; height: 100vh; ">
<app-landing-page [schema]="schema" [changeLogEntries]="changelog"></app-landing-page>
    </div>`,
      props: {
        schema: FILMS_SCHEMA,
        changelog: CHANGELOG_DATA
      }
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
