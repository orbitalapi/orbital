import {NgModule} from '@angular/core';

import {LandingPageComponent} from './landing-page.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import { LandingCardComponent } from './landing-card.component';
import {TuiButtonModule} from '@taiga-ui/core';
import {RouterModule} from '@angular/router';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import { SearchModule } from '../search/search.module';
import { SchemaDiagramModule } from '../schema-diagram/schema-diagram.module';
import { LandingPageContainerComponent } from './landing-page-container.component';
import { ChangelogModule } from '../changelog/changelog.module';

@NgModule({
    imports: [CommonModule, BrowserModule, TuiButtonModule, RouterModule, HeaderBarModule, SearchModule, SchemaDiagramModule, ChangelogModule],
  exports: [LandingPageComponent, LandingCardComponent],
  declarations: [LandingPageComponent, LandingCardComponent, LandingPageContainerComponent],
  providers: [],
})
export class LandingPageModule {
}
