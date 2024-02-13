import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { UiCustomisations } from '../../environments/ui-customisations';
import { ProjectImportComponent } from './project-import.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '', component: ProjectImportComponent, title: `${UiCustomisations.productName}: Projects`
      }
    ])
  ],
})
export class ProjectImportRouteModule {
}
