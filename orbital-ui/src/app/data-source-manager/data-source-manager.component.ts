import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AngularSplitModule } from 'angular-split';
import { Observable } from 'rxjs';
import { UiCustomisations } from '../../environments/ui-customisations';
import { ConnectionsListResponse, DbConnectionService } from '../db-connection-editor/db-importer.service';
import { Schema } from '../services/schema';
import { TypesService } from '../services/types.service';
import { DataSourceTreeComponent } from './data-source-tree/data-source-tree.component';

@Component({
  selector: 'app-data-source-manager',
  template: `
    <as-split direction="horizontal" unit="pixel">
      <as-split-area size="360">
        <ng-container *ngIf="(connections$ | async) as connectionList">
          <div *ngIf="connectionList.definitionsWithErrors.length > 0" class="errors-panel">
            <h3>Some configuration files have errors:</h3>
            <ul>
              <li *ngFor="let error of connectionList.definitionsWithErrors">
                <span>{{ error.identifier.id }}: {{ error.error }}</span>
              </li>
            </ul>
          </div>
        </ng-container>
        <app-data-source-tree [schema$]="schema$" [connections$]="connections$"></app-data-source-tree>
      </as-split-area>
      <as-split-area>
        <router-outlet></router-outlet>
        <div class="no-route-selected">Click on a connection, service or operation on the left to view it's details here</div>
      </as-split-area>
    </as-split>
  `,
  styleUrls: ['./data-source-manager.component.scss'],
  imports: [
    CommonModule,
    AngularSplitModule,
    DataSourceTreeComponent,
    RouterOutlet
  ],
  standalone: true
})
export class DataSourceManagerComponent {
  schema$: Observable<Schema>
  connections$: Observable<ConnectionsListResponse>;

  constructor(
    private typeService: TypesService,
    private dbService: DbConnectionService
  ) {
    this.schema$ = this.typeService.getTypes();
    this.connections$ = this.dbService.getConnections(true);
  }

  protected readonly UiCustomisations = UiCustomisations;
}
