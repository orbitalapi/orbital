import {CommonModule} from '@angular/common';
import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router, RouterOutlet} from '@angular/router';
import {AngularSplitModule} from 'angular-split';
import {Observable, ReplaySubject} from 'rxjs';
import {UiCustomisations} from '../../environments/ui-customisations';
import {ConnectionsListResponse, DbConnectionService} from '../db-connection-editor/db-importer.service';
import {Schema} from '../services/schema';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {TypesService} from '../services/types.service';
import {DataSourceTreeComponent} from './data-source-tree/data-source-tree.component';

@Component({
  selector: 'app-data-source-manager',
  template: `
    <as-split direction="horizontal" unit="pixel">
      <as-split-area size="360">
        <ng-container *ngIf="(connections$ | async) as connectionList">
          <div *ngIf="connectionList.definitionsWithErrors.length > 0" class="errors-panel" (click)="showProblemsPanel()">
            <h3>{{ connectionList.definitionsWithErrors.length }} configuration files have errors</h3>
<!--            <ul>-->
<!--              <li *ngFor="let error of connectionList.definitionsWithErrors">-->
<!--                <h4>{{ error.configFileName}}</h4>-->
<!--                <span>{{ error.identifier.id }}: {{ error.error }}</span>-->
<!--              </li>-->
<!--            </ul>-->
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
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DataSourceManagerComponent {
  schema$: Observable<Schema>
  private connectionsSubject = new ReplaySubject<ConnectionsListResponse>(1);
  connections$ = this.connectionsSubject.asObservable();

  constructor(
    private typeService: TypesService,
    private dbService: DbConnectionService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private schemaNotificationService: SchemaNotificationService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.schemaNotificationService.createSchemaNotificationsSubscription().pipe(
      takeUntilDestroyed()
    )
      .subscribe(() => {
        this.schema$ = this.typeService.getTypes();
        this.dbService.getConnections(true).subscribe(connections => {
          this.connectionsSubject.next(connections);
        });
        this.changeDetectorRef.markForCheck()
      });
  }

  showProblemsPanel() {
    this.router.navigate(['problems'], { relativeTo: this.activatedRoute })
  }

  protected readonly UiCustomisations = UiCustomisations;
}
