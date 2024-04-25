import { Component, Input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs/internal/Observable';
import { ParsedSource } from '../services/schema';
import { ChangeLogEntry, ChangelogService } from '../changelog/changelog.service';
import { SchemaNotificationService } from '../services/schema-notification.service';
import { appInstanceType } from 'src/app/app-config/app-instance.vyne';
import { UiCustomisations } from '../../environments/ui-customisations';

@Component({
  selector: 'app-project-summary-view',
  template: `
    <div class="container">
      <ng-container *ngIf="displayHeaderText">
        <h1>Changelog</h1>
        <p>This is the changelog of all the data sources connected to {{ UiCustomisations.productName }}</p>
        <p>Click on a package on the left to view the schema for that package</p>
      </ng-container>
      <app-changelog-list [changeLogEntries]="changeLogEntries | async"></app-changelog-list>
    </div>
  `,
  styleUrls: ['./project-summary-view.component.scss'],
  host: {'class': appInstanceType.appType}
})
export class ProjectSummaryViewComponent {
  @Input()
  displayHeaderText: boolean = true;

  schemas: Observable<ParsedSource[]>;
  changeLogEntries: Observable<ChangeLogEntry[]>
  protected readonly UiCustomisations = UiCustomisations;

  constructor(
    private changelogService: ChangelogService,
    private schemaNotificationService: SchemaNotificationService) {
    this.loadSchemas();
    this.schemaNotificationService.createSchemaNotificationsSubscription().pipe(
      takeUntilDestroyed()
    )
      .subscribe(() => {
        this.loadSchemas();
      });
  }

  private loadSchemas() {
    this.changeLogEntries = this.changelogService.getChangelog();
  }

}
