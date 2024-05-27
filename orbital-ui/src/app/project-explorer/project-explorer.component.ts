import {ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, OnInit} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router} from '@angular/router';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {
  PackagesService,
  PackageWithDescription,
  ParsedPackage,
  SourcePackageDescription
} from '../package-viewer/packages.service';
import {Badge} from '../simple-badge-list/simple-badge-list.component';
import moment from 'moment';
import {ChangeLogEntry, ChangelogService} from 'src/app/changelog/changelog.service';
import {Observable} from 'rxjs';
import {TypesService} from 'src/app/services/types.service';
import {ParsedSource, PartialSchema, Schema} from 'src/app/services/schema';
import {appInstanceType} from 'src/app/app-config/app-instance.vyne';
import {integer} from "vscode-languageclient";
import {isNullOrUndefined} from "../utils/utils";
import {FileTreeNode, sourcesToFileTreeNode} from "../code-viewer/file-tree.component";

@Component({
  selector: 'app-project-explorer',
  templateUrl: './project-explorer.component.html',
  styleUrls: ['./project-explorer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {'class': appInstanceType.appType}
})
export class ProjectExplorerComponent implements OnInit {


  packageWithDescription: PackageWithDescription
  tabs = [
    {label: 'Schema', icon: 'assets/img/tabler/table.svg', route: 'schema'},
    {label: 'Changelog', icon: 'assets/img/tabler/git-pull-request.svg', route: 'changelog'},
    {label: 'Source', icon: 'assets/img/tabler/code.svg', route: 'source'},
    {label: 'Settings', icon: 'assets/img/tabler/settings.svg', route: 'settings'}
  ]

  get packageDescription(): SourcePackageDescription {
    return this.packageWithDescription?.description;
  }

  setActiveTab(index: integer) {
    const newRoute = this.tabs[index].route;
    this.router.navigate(['..', newRoute], {relativeTo: this.activatedRoute})
  }


  badges: Badge[] = [];

  partialSchema$: Observable<PartialSchema>;

  activeTabIndex: number = 0;

  schema: Schema;

  get parsedPackage(): ParsedPackage {
    return this.packageWithDescription?.parsedPackage
  }

  private _fileTreeNodes: FileTreeNode[] = [];

  /**
   * Returns all the sources in the package (include "additionalSources") as a tree
   */
  get sources(): FileTreeNode[] {
    return this._fileTreeNodes;
  }

  constructor(private packagesService: PackagesService,
              private schemaNotificationService: SchemaNotificationService,
              private activatedRoute: ActivatedRoute,
              private changeDetector: ChangeDetectorRef,
              private changelogService: ChangelogService,
              private typeService: TypesService,
              private router: Router,
              private destroyRef: DestroyRef
  ) {
    this.activatedRoute.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(
        paramMap => {
          const selectedTab = paramMap.get('selectedTab');
          if (!selectedTab) {
            this.router.navigate(
              [this.tabs[0].route],
              {relativeTo: this.activatedRoute, onSameUrlNavigation: "reload", skipLocationChange: true}
            )
          } else {
            this.activeTabIndex = this.tabs.findIndex(tab => tab.route === selectedTab);
            this.changeDetector.markForCheck();
          }
        }
      )
  }

  ngOnInit() {
    this.typeService.getTypes()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(schema => this.schema = schema);
    this.schemaNotificationService.createSchemaNotificationsSubscription()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.loadPackages();
      });
  }

  changelogEntries: Observable<ChangeLogEntry[]>

  private loadPackages() {
    const packageName = this.activatedRoute.snapshot.paramMap.get('packageName');
    this.packagesService.loadPackage(packageName)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: packageWithDescription => {
          this.packageWithDescription = packageWithDescription;
          this.updateBadges();
          this.updateFileTree();
          this.changeDetector.markForCheck();
        },
        // there's a good chance that project no longer exists, pull the ripcord and eject back to the /projects route
        error: () => this.router.navigate(['/projects'])
      });
    this.partialSchema$ = this.packagesService.getPartialSchemaForPackage(packageName);
    this.changelogEntries = this.changelogService.getChangelogForPackage(packageName);
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

  private updateFileTree() {
    if (isNullOrUndefined(this.parsedPackage)) {
      return [];
    } else {
      const projectSources = sourcesToFileTreeNode(this.parsedPackage.sources, 'Taxi sources');
      const additionalSources = Object.keys(this.parsedPackage.additionalSources).map(additionalSourceKey => {
        const sources = this.parsedPackage.additionalSources[additionalSourceKey];
        const fileTreeNode = sourcesToFileTreeNode(sources, additionalSourceKey);
        return fileTreeNode;
      });
      const newFileTreeNodes = [projectSources];

      if (additionalSources.length > 0) {
        const additionalSourcesRoot: FileTreeNode = new FileTreeNode('Additional sources', null);
        additionalSources.forEach(additionalSourcesTreeNode => additionalSourcesRoot.addChild(additionalSourcesTreeNode))
        newFileTreeNodes.push(additionalSourcesRoot);
      }

      this._fileTreeNodes = newFileTreeNodes;
    }
  }
}
