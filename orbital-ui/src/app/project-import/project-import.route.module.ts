import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { UiCustomisations } from '../../environments/ui-customisations';
import { ProjectImportComponent } from './project-import.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '', component: ProjectImportComponent, title: `${UiCustomisations.productName}: Add a Project`
      },
      {
        path: 'git-repository', component: ProjectImportComponent, title: `${UiCustomisations.productName}: Add project from Git repository`
      },
      {
        path: 'local-disk', component: ProjectImportComponent, title: `${UiCustomisations.productName}: Add project from local disk`
      },
      {
        path: 'data-source', component: ProjectImportComponent, title: `${UiCustomisations.productName}: Add a Data source`, children: [{
          path: 'configure',
          component: ProjectImportComponent,
          title: `${UiCustomisations.productName}: Configure a Data source`
        }]
      }
    ])
  ],
})
export class ProjectImportRouteModule {
}
