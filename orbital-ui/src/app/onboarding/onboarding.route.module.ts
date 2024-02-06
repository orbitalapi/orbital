import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { OnboardingContainerComponent } from './onboarding-container.component';
import { CreateProjectComponent } from './create-project/create-project.component';
import { CreateDataSourceComponent } from './create-data-source/create-data-source.component';
import { ExploreComponent } from './explore/explore.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '', component: OnboardingContainerComponent, children: [
          { path: 'project', component: CreateProjectComponent },
          { path: 'data-source', component: CreateDataSourceComponent },
          { path: 'explore', component: ExploreComponent },
        ]
      }
    ])
  ],
})
export class OnboardingRouteModule {
}
