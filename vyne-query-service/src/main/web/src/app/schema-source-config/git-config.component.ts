import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import {
  ConvertSchemaEvent,
  GitPullRequestConfig,
  GitRepositoryConfig,
  LoadablePackageType,
  OpenApiPackageLoaderSpec,
  TaxiPackageLoaderSpec
} from 'src/app/schema-importer/schema-importer.models';
import { isNullOrUndefined } from 'util';
import { GitConnectionTestResult, SchemaImporterService } from 'src/app/schema-importer/schema-importer.service';
import { Message } from 'src/app/services/schema';


export const projectTypeToString = (item: LoadablePackageType) => {
  switch (item) {
    case 'Taxi':
      return 'Taxi';
    case 'OpenApi':
      return 'Open API'
  }
}

@Component({
  selector: 'app-git-config',
  styleUrls: ['./git-config.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="form-header-text">
      <p>Connect Orbital to a Git repository to add individual OpenAPI schemas, or entire Taxi projects</p>
    </div>

    <form #gitForm="ngForm">
      <div class="form-container">
        <div class="form-body">
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Repository URL</h3>
              <div class="help-text">
                <p>
                  Provide the repository URL - https:// is recommended.
                </p>
                <p>
                  Gitlab and Github personal access tokens are supported.
                </p>
              </div>
            </div>
            <div class="form-element">
              <div class="row">
                <tui-input [(ngModel)]="gitConfig.uri" (change)="updateRepositoryName($event)" required
                           name="gitUri"
                           [readOnly]="!editable"
                           class="flex-grow">
                  Repository URL
                </tui-input>
                <button tuiButton appearance="outline" size="m"
                        [disabled]="testingConnection || !gitConfig.uri"
                        [showLoader]="testingConnection" (click)="testConnection()">Test connection
                </button>
              </div>
              <div class="test-result-box error-message"
                   *ngIf="!testingConnection && connectionTestResult && !connectionTestResult.successful">{{ connectionTestResult.errorMessage }}</div>
              <div class="test-result-box success-message"
                   *ngIf="!testingConnection && connectionTestResult && connectionTestResult.successful ">
                Connection tested successfully
              </div>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Repository name</h3>
              <div class="help-text">
                The repository name. We'll guess this from the URL, or you can pick something meaningful.
              </div>
            </div>
            <div class="form-element">
              <tui-input [(ngModel)]="gitConfig.name" name="repositoryName" required [readOnly]="!editable">
                Repository name
              </tui-input>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Branch</h3>
              <div class="help-text">
                <p>
                  The branch to monitor
                </p>
              </div>
            </div>
            <div class="form-element">
              <div style="flex-grow: 1">
                <tui-combo-box
                  [disabled]="availableBranches === null"
                  [readOnly]="!editable"
                  [(ngModel)]="gitConfig.branch" name="branch" required>
                  Branch
                  <tui-data-list *tuiDataList>
                    <button tuiOption *ngFor="let branchName of availableBranches"
                            [value]="branchName">{{ branchName }}</button>
                  </tui-data-list>
                </tui-combo-box>
                <p *ngIf="availableBranches === null" class="help-text" style="width: 100%">Test your git
                  connection to see a list of available branches</p>
              </div>
            </div>
          </div>
          <div class="form-row">
            <div class="form-item-description-container">
              <h3>Project type</h3>
              <div class="help-text">
                Git repositories can contain full Taxi projects, or individual API specs.
              </div>
            </div>
            <div class="form-element">
              <tui-select
                [stringify]="stringifyProjectType"
                [ngModel]="gitConfig.loader.packageType"
                [readOnly]="!editable"
                (ngModelChange)="selectedProjectTypeChanged($event)"
                name="project-type" required
              >
                Project type
                <tui-data-list *tuiDataList>
                  <button tuiOption value="Taxi">{{ stringifyProjectType('Taxi') }}</button>
                  <button tuiOption value="OpenApi">{{ stringifyProjectType('OpenApi')}}</button>
                </tui-data-list>
              </tui-select>
            </div>
          </div>
          <ng-container *ngIf="gitConfig.loader.packageType === 'OpenApi'">
            <app-open-api-package-config [openApiPackageSpec]="openApiPackageSpec"
                                         [(path)]="gitConfig.path"></app-open-api-package-config>

          </ng-container>

          <ng-container *ngIf="gitConfig.loader.packageType === 'Taxi'">
            <div class="form-row">
              <div class="form-item-description-container">
                <h3>Path to taxi project</h3>
                <div class="help-text">
                  Specify the path (from the root of the git repository) to the directory containing a
                  <code>taxi.conf</code> file
                </div>
              </div>
              <div class="form-element">
                <div class="row">
                  <div style="flex-grow: 1;">
                    <tui-input [(ngModel)]="gitConfig.path" class="flex-grow" name="pathToTaxi" [readOnly]="!editable"
                               required>
                      Path
                    </tui-input>
                    <p class="help-text" style="width: 100%;">We'll look for a Taxi config file at
                      <code>{{expectedTaxiConfLocation}}</code></p>
                  </div>
                </div>

              </div>
            </div>
            <div class="form-row">
              <div class="form-item-description-container">
                <h3>Enable edits and pull requests</h3>
                <div class="help-text">
                  <p>
                    If enabled, edits can be made through the Orbital UI, which
                    will result in Pull requests being opened
                  </p>
                </div>
              </div>
              <div class="form-element">
                <tui-checkbox [(ngModel)]="gitConfig.isEditable" name="editable" required [readOnly]="!editable"
                              (change)="onEditableChanged($event)"></tui-checkbox>
              </div>
            </div>
            <div class="form-row" *ngIf="gitConfig.isEditable && gitConfig.pullRequestConfig">
              <div class="form-item-description-container">
                <h3>Prefix for pull requests</h3>
                <div class="help-text">
                  <p>
                    Edits will be made on a branch, and then a pull request raised. Pull requests and
                    branches
                    will have this prefix
                  </p>
                </div>
              </div>
              <div class="form-element">
                <tui-input [(ngModel)]="gitConfig.pullRequestConfig.branchPrefix" [readOnly]="!editable"
                           name="branchPrefix">
                  Prefix
                </tui-input>
              </div>
            </div>
          </ng-container>
        </div>
      </div>
    </form>
    <div class="form-button-bar" *ngIf="editable">
      <button tuiButton [showLoader]="working" [size]="'m'" (click)="doCreate()" [disabled]="gitForm.invalid">Create
      </button>
    </div>
    <tui-notification [status]="saveResultMessage.level.toLowerCase()" *ngIf="saveResultMessage">
      {{ saveResultMessage.message }}
    </tui-notification>
  `
})
export class GitConfigComponent {

  testingConnection = false;
  saveResultMessage: Message;
  readonly stringifyProjectType = projectTypeToString;
  working = false;
  @Input()
  gitConfig = new GitRepositoryConfig();
  connectionTestResult: GitConnectionTestResult | null = null;
  @Input()
  editable: boolean = true;
  @Output()
  loadSchema = new EventEmitter<ConvertSchemaEvent>()

  constructor(private schemaService: SchemaImporterService, private changeDetector: ChangeDetectorRef) {
  }

  get expectedTaxiConfLocation(): string {
    if (isNullOrUndefined(this.gitConfig.path)) {
      return ''
    } else {
      const seperator = this.gitConfig.path.endsWith('/') ? '' : '/'
      return this.gitConfig.path + seperator + 'taxi.conf'
    }
  }

  get availableBranches(): string[] | null {
    return this.connectionTestResult ? this.connectionTestResult.branchNames : null;
  }

  get openApiPackageSpec(): OpenApiPackageLoaderSpec | null {
    const packageType = this.gitConfig.loader?.packageType;
    if (packageType === 'OpenApi') {
      return this.gitConfig.loader as OpenApiPackageLoaderSpec;
    } else {
      return null;
    }
  }

  selectedProjectTypeChanged(projectType: LoadablePackageType) {
    switch (projectType) {
      case 'Taxi':
        this.gitConfig.loader = new TaxiPackageLoaderSpec();
        break;
      case 'OpenApi':
        this.gitConfig.loader = new OpenApiPackageLoaderSpec();
        break;
    }
    this.changeDetector.markForCheck();
  }

  updateRepositoryName($event) {
    if (!this.gitConfig.uri) {
      return;
    }
    const repoNameParts = this.gitConfig.uri.split('/')
    const repoName = repoNameParts[repoNameParts.length - 1]

    if (repoName.endsWith('.git')) {
      this.gitConfig.name = repoName.substring(0, repoName.length - 4);
    } else {
      this.gitConfig.name = repoName;
    }
  }

  onEditableChanged(event) {
    if (this.gitConfig.isEditable && isNullOrUndefined(this.gitConfig.pullRequestConfig)) {
      this.gitConfig.pullRequestConfig = new GitPullRequestConfig();
    }
  }


  doCreate() {
    console.log(JSON.stringify(this.gitConfig, null, 2));
    this.working = true;
    this.saveResultMessage = null;
    this.schemaService.addNewGitRepository(this.gitConfig)
      .subscribe(result => {
          this.working = false;
          this.saveResultMessage = {
            message: 'The git repository was added successfully',
            level: 'SUCCESS',
          };
          this.changeDetector.markForCheck();
        },
        error => {
          console.log(JSON.stringify(error))
          this.working = false;
          this.saveResultMessage = {
            message: 'There was a problem adding the git repository',
            level: 'ERROR'
          }
          this.changeDetector.markForCheck();
        })
  }

  testConnection() {
    if (!this.gitConfig?.uri) {
      console.log('No uri is provided')
    }
    this.testingConnection = true;
    this.schemaService.testGitConnection({
      uri: this.gitConfig.uri
    }).subscribe(result => {
      this.testingConnection = false
      this.connectionTestResult = result;
      this.gitConfig.branch = result.defaultBranch;
      this.changeDetector.markForCheck();
    })
  }
}
