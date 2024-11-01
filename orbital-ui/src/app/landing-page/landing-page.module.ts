import { TuiButton } from "@taiga-ui/core";
import {NgModule} from '@angular/core';

import {LandingPageComponent} from './landing-page.component';
import {CommonModule} from '@angular/common';
import {LandingCardComponent} from './landing-card.component';
import {RouterModule} from '@angular/router';
import {HeaderBarModule} from '../header-bar/header-bar.module';
import {SearchModule} from '../search/search.module';
import {SchemaDiagramModule} from '../schema-diagram/schema-diagram.module';
import {LandingPageContainerComponent} from './landing-page-container.component';
import { OnboardingContainerComponent } from '../onboarding/onboarding-container.component';

/**
 * @deprecated superseded by the Dashboard
 */
@NgModule({
    imports: [
        CommonModule,
        TuiButton,
        RouterModule,
        HeaderBarModule,
        SearchModule,
        SchemaDiagramModule, OnboardingContainerComponent,
    ],
  exports: [LandingPageComponent, LandingCardComponent],
  declarations: [LandingPageComponent, LandingCardComponent, LandingPageContainerComponent],
  providers: [],
})
export class LandingPageModule {
}
