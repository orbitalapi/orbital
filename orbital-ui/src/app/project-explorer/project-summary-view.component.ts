import {Component, OnInit} from '@angular/core';
import {Observable} from 'rxjs/internal/Observable';
import {ParsedSource, VersionedSource} from '../services/schema';
import {AppInfoService, AppConfig} from '../services/app-info.service';
import {PackagesService, SourcePackageDescription} from '../package-viewer/packages.service';
import {ChangeLogEntry, ChangelogService} from '../changelog/changelog.service';
import {TypesService} from '../services/types.service';
import {ActivatedRoute, Router} from '@angular/router';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {appInstanceType} from 'src/app/app-config/app-instance.vyne';

@Component({
  selector: 'app-project-summary-view',
  template: `
    <div class="container">
      <h1>Changelog</h1>
      <p>This is the changelog of all the data sources connected to Vyne.</p>
      <p>Click on a package on the left to view the schema for that package</p>
      <app-changelog-list [changeLogEntries]="changeLogEntries | async"></app-changelog-list>
    </div>
  `,
  styleUrls: ['./project-summary-view.component.scss'],
  host: {'class': appInstanceType.appType}
})
export class ProjectSummaryViewComponent {

  schemas: Observable<ParsedSource[]>;

  changeLogEntries: Observable<ChangeLogEntry[]>

  constructor(
    private changelogService: ChangelogService,
    private schemaNotificationService: SchemaNotificationService) {
    this.loadSchemas();
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(() => {
        this.loadSchemas();
      });
  }

  private loadSchemas() {
    this.changeLogEntries = this.changelogService.getChangelog();
  }
}
