import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SchemaNotificationService } from '../services/schema-notification.service';
import { PackagesService, ParsedPackage } from '../package-viewer/packages.service';
import { Badge } from '../simple-badge-list/simple-badge-list.component';
import { MomentModule } from 'ngx-moment';
import * as moment from 'moment';

@Component({
  selector: 'app-schema-explorer',
  templateUrl: './schema-explorer.component.html',
  styleUrls: ['./schema-explorer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchemaExplorerComponent implements OnInit {


  parsedPackage: ParsedPackage;
  badges: Badge[] = [];

  constructor(private packagesService: PackagesService,
              private schemaNotificationService: SchemaNotificationService,
              private activatedRoute: ActivatedRoute,
              private changeDetector: ChangeDetectorRef,
  ) {

  }

  ngOnInit() {
    this.loadSchemas();
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .subscribe(() => {
        this.loadSchemas();
      });
  }

  private loadSchemas() {
    this.activatedRoute.paramMap.subscribe(paramMap => {
      const packageName = paramMap.get('packageName')
      this.packagesService.loadPackage(packageName)
        .subscribe(parsedPackage => {
          this.parsedPackage = parsedPackage;
          this.updateBadges();
          this.changeDetector.markForCheck();
        });
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
      { label: 'Last published', value: moment(this.parsedPackage.metadata.submissionDate).fromNow(), iconPath: 'assets/img/tabler/clock.svg' },
    ]
  }
}
