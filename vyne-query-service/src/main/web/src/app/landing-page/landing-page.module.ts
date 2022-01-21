import {NgModule} from '@angular/core';

import {LandingPageComponent} from './landing-page.component';
import {CommonModule} from '@angular/common';
import {BrowserModule} from '@angular/platform-browser';
import { LandingCardComponent } from './landing-card.component';
import {TuiButtonModule} from '@taiga-ui/core';
import {RouterModule} from '@angular/router';
import {HeaderBarModule} from '../header-bar/header-bar.module';

@NgModule({
  imports: [CommonModule, BrowserModule, TuiButtonModule, RouterModule, HeaderBarModule],
  exports: [LandingPageComponent, LandingCardComponent],
  declarations: [LandingPageComponent, LandingCardComponent],
  providers: [],
})
export class LandingPageModule {
}
