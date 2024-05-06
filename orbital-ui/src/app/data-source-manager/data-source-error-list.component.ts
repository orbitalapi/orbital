import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {PackagesService, ProjectLoaderWithStatus} from "../package-viewer/packages.service";
import {Observable} from "rxjs";
import {ConnectionsListResponse, DbConnectionService} from "../db-connection-editor/db-importer.service";
import {HeaderComponentLayoutModule} from "../header-component-layout/header-component-layout.module";
import {AsyncPipe, NgForOf} from "@angular/common";

@Component({
  selector: 'app-data-source-error-list',
  template: `
    <app-header-component-layout
      title="Data sources - problems"
      description="There are problems with these data source configuration files, preventing them from loading.">
      <div class="project-list-container">
        <table class="project-list">
          <thead>
          <tr>
            <th>Project</th>
            <th>File</th>
            <th>Error</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let connection of (connections$ | async)?.definitionsWithErrors">
            <td>{{ connection.identifier.id }}</td>
            <td>{{ connection.configFileName }}</td>
            <td>{{ connection.error }}</td>
          </tr>
          </tbody>
        </table>
      </div>

    </app-header-component-layout>
  `,
  styleUrls: ['./data-source-error-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    HeaderComponentLayoutModule,
    NgForOf,
    AsyncPipe
  ],
  standalone: true
})
export class DataSourceErrorListComponent {

  readonly connections$: Observable<ConnectionsListResponse>;

  constructor(
              private dbService: DbConnectionService,) {
    this.connections$ = this.dbService.getConnections(true);
  }


}
