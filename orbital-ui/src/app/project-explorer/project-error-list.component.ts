import {ChangeDetectionStrategy, Component} from '@angular/core';
import {PackagesService, ProjectLoaderWithStatus} from "../package-viewer/packages.service";
import {Observable} from "rxjs";
import {AsyncPipe, CommonModule} from '@angular/common';
import {HeaderComponentLayoutComponent} from '../header-component-layout/header-component-layout.component';
import { map } from "rxjs/operators";

@Component({
    selector: 'app-project-error-list',
    template: `
    <app-header-component-layout
      title="Projects - problems"
      description="These projects have issues. Errors prevent loading; warnings mean the project loaded from a local cache but could not sync with the remote.">
      <div class="project-list-container">
        <table class="project-list">
          <thead>
          <tr>
            <th>Project</th>
            <th>Severity</th>
            <th>Status</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let project of (unhealthyLoaders$ | async)" [ngClass]="project.status.state.toLowerCase()">
            <td>{{ project.loaderDescription }}</td>
            <td class="severity-cell">
              <span class="severity-badge" [ngClass]="project.status.state.toLowerCase()">
                {{ project.status.state === 'ERROR' ? 'Error' : 'Warning' }}
              </span>
            </td>
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
      HeaderComponentLayoutComponent
    ]
})
export class ProjectErrorListComponent {

  readonly unhealthyLoaders$: Observable<ProjectLoaderWithStatus[]>;

  constructor(private packagesService: PackagesService,) {
    this.unhealthyLoaders$ = this.packagesService.loadUnhealthyProjects()
      .pipe(map(e => e.unhealthyLoaders))
  }


}
