import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { OnboardingContainerComponent } from './onboarding-container.component';
import { CreateProjectComponent } from './create-project/create-project.component';
import { CreateDataSourceComponent } from './create-data-source/create-data-source.component';
import { ExploreComponent } from './explore/explore.component';
import { UiCustomisations } from '../../environments/ui-customisations';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '', component: OnboardingContainerComponent, children: [
          {
            path: 'project',
            component: CreateProjectComponent,
            title: `${UiCustomisations.productName}: Onboarding > Create Project`
          },
          {
            path: 'data-source',
            component: CreateDataSourceComponent,
            title: `${UiCustomisations.productName}: Onboarding > Create Data source`
          },
          {
            path: 'explore',
            component: ExploreComponent,
            title: `${UiCustomisations.productName}: Onboarding > Explore`
          },
        ]
      }
    ])
  ],
})
export class OnboardingRouteModule {
}
