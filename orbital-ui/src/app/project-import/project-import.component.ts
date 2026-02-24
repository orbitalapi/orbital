import { Component } from '@angular/core';
import { HeaderComponentLayoutComponent } from '../header-component-layout/header-component-layout.component';
import { AddProjectComponent } from '../onboarding/add-project/add-project.component';

@Component({
  selector: 'app-project-import',
  standalone: true,
  imports: [
    HeaderComponentLayoutComponent,
    AddProjectComponent
  ],
  template: `
    <app-header-component-layout
      title="Add a Project"
      description="Projects are where you define your data sources, and hold your API specs."
    >
      <app-add-project [isOnboardingMode]="false"></app-add-project>
    </app-header-component-layout>
  `,
  styleUrls: ['./project-import.component.scss'],
})
export class ProjectImportComponent {
}
