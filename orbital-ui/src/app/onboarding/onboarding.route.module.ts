import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { OnboardingContainerComponent } from './onboarding-container.component';
import { AddProjectComponent } from './add-project/add-project.component';
import { DataSourceComponent } from './data-source/data-source.component';
import { ExploreComponent } from './explore/explore.component';
import { UiCustomisations } from '../../environments/ui-customisations';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '', component: OnboardingContainerComponent, children: [
          {
            path: 'project',
            component: AddProjectComponent,
            title: `${UiCustomisations.productName}: Onboarding > Add project`
          },
          {
            path: 'data-source',
            component: DataSourceComponent,
            title: `${UiCustomisations.productName}: Onboarding > Select data source`,
            children: [
              {
                path: 'configure',
                component: DataSourceComponent,
                title: `${UiCustomisations.productName}: Onboarding > Configure`
              }
            ]
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
