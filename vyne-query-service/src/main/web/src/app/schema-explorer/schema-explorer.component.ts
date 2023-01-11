import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SchemaNotificationService } from '../services/schema-notification.service';
import { PackagesService, ParsedPackage } from '../package-viewer/packages.service';
import { Badge } from '../simple-badge-list/simple-badge-list.component';
import * as moment from 'moment';
import { ChangeLogEntry, ChangelogService } from 'src/app/changelog/changelog.service';
import { Observable } from 'rxjs';
import { TypesService } from 'src/app/services/types.service';
import { PartialSchema, Schema } from 'src/app/services/schema';
import { AppType } from 'src/app/app-config/app-type';
import { appInstanceType } from 'src/app/app-config/app-instance.vyne';

@Component({
  selector: 'app-schema-explorer',
  templateUrl: './schema-explorer.component.html',
  styleUrls: ['./schema-explorer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {'class': appInstanceType.appType }
})
export class SchemaExplorerComponent implements OnInit {


  parsedPackage: ParsedPackage;
  badges: Badge[] = [];

  partialSchema$: Observable<PartialSchema>;

  activeTabIndex: number = 0;

  schema: Schema;

  tabs = [
    { label: 'Browse', icon: 'assets/img/tabler/table.svg' },
    { label: 'Changelog', icon: 'assets/img/tabler/git-pull-request.svg' },
    { label: 'Source', icon: 'assets/img/tabler/code.svg' }
  ]

  constructor(private packagesService: PackagesService,
              private schemaNotificationService: SchemaNotificationService,
              private activatedRoute: ActivatedRoute,
              private changeDetector: ChangeDetectorRef,
              private changelogService: ChangelogService,
              private typeService: TypesService
  ) {

  }

  ngOnInit() {
    this.typeService.getTypes()
      .subscribe(schema => this.schema = schema);
    this.loadPackages();
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(() => {
        this.loadPackages();
      });
  }

  changelogEntries: Observable<ChangeLogEntry[]>

  private loadPackages() {
    this.activatedRoute.paramMap.subscribe(paramMap => {
      const packageName = paramMap.get('packageName')
      this.packagesService.loadPackage(packageName)
        .subscribe(parsedPackage => {
          this.parsedPackage = parsedPackage;
          this.updateBadges();
          this.changeDetector.markForCheck();
        });
      this.partialSchema$ = this.packagesService.getPartialSchemaForPackage(packageName);
      this.changelogEntries = this.changelogService.getChangelogForPackage(packageName);
    })
  }

  private updateBadges() {

    this.badges = [
      {
        label: 'Organisation',
        value: this.parsedPackage.metadata.identifier.organisation,
        iconPath: 'assets/img/tabler/affiliate.svg'
      },
      {
        label: 'Version',
        value: this.parsedPackage.metadata.identifier.version,
        iconPath: 'assets/img/tabler/versions.svg'
      },
      {
        label: 'Last published',
        value: moment(this.parsedPackage.metadata.submissionDate).fromNow(),
        iconPath: 'assets/img/tabler/clock.svg'
      },
    ]
  }
}
