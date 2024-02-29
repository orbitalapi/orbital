import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { OnboardingContainerComponent } from './onboarding-container.component';
import { AddProjectComponent } from './add-project/add-project.component';
import { CreateDataSourceComponent } from './select-data-source/create-data-source.component';
import { ExploreComponent } from './explore/explore.component';
import { UiCustomisations } from '../../environments/ui-customisations';
import { ConfigureDataSourceComponent } from './configure-data-source/configure-data-source.component';

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
            component: CreateDataSourceComponent,
            title: `${UiCustomisations.productName}: Onboarding > Select data source`
          },
          {
            path: 'configure',
            component: ConfigureDataSourceComponent,
            title: `${UiCustomisations.productName}: Onboarding > Configure`
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
  declarations: [
    ConfigureDataSourceComponent
  ],
})
export class OnboardingRouteModule {
}
