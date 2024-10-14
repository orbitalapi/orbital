import {ChangeDetectionStrategy, Component} from '@angular/core';
import {PackagesService, ProjectLoaderWithStatus} from "../package-viewer/packages.service";
import {Observable} from "rxjs";
import {AsyncPipe, CommonModule} from '@angular/common';
import {HeaderComponentLayoutModule} from '../header-component-layout/header-component-layout.module';

@Component({
    selector: 'app-project-error-list',
    template: `
    <app-header-component-layout
      title="Projects - problems"
      description="There are problems with these projects, preventing them from loading.">
      <div class="project-list-container">
        <table class="project-list">
          <thead>
          <tr>
            <th>Project</th>
            <th>Status</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let project of (unhealthyLoaders$ | async)">
            <td>{{ project.loaderDescription }}</td>
            <td>{{ project.status.message }}</td>
          </tr>
          </tbody>
        </table>
      </div>

    </app-header-component-layout>
  `,
    styleUrls: ['./project-error-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [
      CommonModule,
      AsyncPipe,
      HeaderComponentLayoutModule
    ]
})
export class ProjectErrorListComponent {

  readonly unhealthyLoaders$: Observable<ProjectLoaderWithStatus[]>;

  constructor(private packagesService: PackagesService,) {
    this.unhealthyLoaders$ = this.packagesService.loadProjectLoadersWithErrors()
  }


}
