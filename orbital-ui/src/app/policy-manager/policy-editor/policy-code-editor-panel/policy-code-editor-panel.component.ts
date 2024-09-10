import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  Inject,
  Injector,
  Input,
  Output,
} from '@angular/core';
import {Router} from '@angular/router';
import {ExpandingPanelSetModule} from "../../../expanding-panelset/expanding-panel-set.module";
import {AngularSplitModule} from "angular-split";
import {CodeEditorModule} from "../../../code-editor/code-editor.module";
import {CompilationMessageListModule} from "../../../compilation-message-list/compilation-message-list.module";
import {NgIf} from "@angular/common";
import {
  TuiAlertService,
  TuiButtonModule,
  TuiDialogService,
  TuiNotificationModule
} from '@taiga-ui/core';
import {isNullOrUndefined} from "../../../utils/utils";
import {
  CreateOrReplaceSource,
  SavedQueryWithSource,
  SchemaEdit,
  SchemaImporterService
} from "../../../project-import/schema-importer.service";
import {PolymorpheusComponent} from "@tinkoff/ng-polymorpheus";
import {
  SaveQueryDialogComponent,
  SaveQueryRequestProps
} from "../../../query-panel/query-editor/query-editor-toolbar/save-query-dialog.component";
import {VersionedSource} from "../../../services/schema";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {SaveWithFilenameComponent} from "../../../filename-display/save-with-filename.component";
import {PoliciesService, PolicySetupReadiness} from "../../../services/policies.service";
import {TuiAccordionModule} from "@taiga-ui/kit";
import {JsonViewerModule} from "../../../json-viewer/json-viewer.module";
import {toSourceWithTypeHints} from "../../../model-designer/taxi-parser.service";

@Component({
  selector: 'app-policy-code-editor-panel',
  standalone: true,
  imports: [
    ExpandingPanelSetModule,
    AngularSplitModule,
    CodeEditorModule,
    CompilationMessageListModule,
    NgIf,
    TuiButtonModule,
    SaveWithFilenameComponent,
    TuiAccordionModule,
    JsonViewerModule,
    TuiNotificationModule
  ],
  template: `
    <app-panel-header title="Policy editor" [isSecondary]="true">
      <div class="spacer"></div>
      <app-save-with-filename (saveFile)="saveFile()" [source]="versionedSource"
                              [isDisabled]="!source"></app-save-with-filename>
    </app-panel-header>
    <tui-notification (close)="showPolicyEditorHelp = false" status="info" *ngIf="showPolicyEditorHelp">
      <div>Create a policy here, and then ensure the output is correct by running a query against it.</div>
    </tui-notification>
    <div class="code-editor-container">
      <app-code-editor
        [content]="source"
        [showCompilationProblemsPanel]="true"
        (contentChange)="sourceChange($event)"
      ></app-code-editor>
    </div>
    <tui-accordion [rounded]="false" [class.expanded]="authTokenOpen">
      <tui-accordion-item class="accordion-with-panel-header" [(open)]="authTokenOpen">
        <app-panel-header title="Auth Token:" [isSecondary]="true">
          <span class="mono-badge auth-token-label">{{ userTokenTypeName }}<span class="badge model">Model</span></span>
        </app-panel-header>
        <ng-template tuiAccordionItemContent>
          <tui-notification (close)="showAuthTokenHelp = false" status="info" *ngIf="showAuthTokenHelp">
            <div>This is the contents of your auth token. We're showing it here for reference while you build your policy.</div>
          </tui-notification>
          <app-json-viewer [json]="userTokenWithTypeHints" [readOnly]="true" [showHeader]="false"></app-json-viewer>
        </ng-template>
      </tui-accordion-item>
    </tui-accordion>
  `,
  styleUrl: './policy-code-editor-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PolicyCodeEditorPanelComponent {
  private policySetup: PolicySetupReadiness;

  authTokenOpen = true

  source: string = '';
  showPolicyEditorHelp: boolean = true;
  showAuthTokenHelp: boolean = true;

  private _versionedSource: VersionedSource = null;
  @Input()
  get versionedSource(): VersionedSource {
    return this._versionedSource;
  }

  set versionedSource(value: VersionedSource) {
    if (isNullOrUndefined(value)) return;
    this._versionedSource = value;
    this.source = value.content
  }

  @Output()
  policyNeedsSaving: EventEmitter<boolean> = new EventEmitter<boolean>();

  get userTokenWithTypeHints() {
    if (!this.policySetup) return null
    const firstKey =  Object.keys(this.policySetup.authTokenInstances)[0]
    return toSourceWithTypeHints(this.policySetup.authTokenInstances[firstKey])
  }
  get userTokenTypeName():string {
    if (!this.policySetup) return null
    const firstKey =  Object.keys(this.policySetup.authTokenInstances)?.[0]

    return firstKey;
  }

  constructor(@Inject(TuiDialogService) private readonly tuiDialogService: TuiDialogService,
              @Inject(Injector) private readonly injector: Injector,
              @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
              private destroyRef: DestroyRef,
              private schemaImporterService: SchemaImporterService,
              private policiesService: PoliciesService,
              private changeDetector: ChangeDetectorRef,
              private router: Router
  ) {
    policiesService.getPolicySetupReadiness().pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (!Object.keys(value.authTokenInstances).length) {
        // the auth token credentials are not setup yet
        this.router.navigate(['/policies'])
        return;
      }
      this.policySetup = value;
      this.changeDetector.markForCheck();
    })
  }

  sourceChange($event: string) {
    if (this.source !== $event) {
      this.policyNeedsSaving.emit(true);
    }
    this.source = $event;
  }

  saveFile() {
    if (isNullOrUndefined(this.versionedSource)) {
      this.saveNewFile();
    } else {
      this.saveExistingFile();
    }
  }

  private saveNewFile() {
    this.tuiDialogService.open<SavedQueryWithSource>(new PolymorpheusComponent(SaveQueryDialogComponent, this.injector),
      {
        size: 'l',
        data: {
          query: this.source,
          label: 'policy',
          previousVersion: null,
          existingSavedQueryNames: [],
          schemaEditBuilder: (packageId, filename) => {
            const source: VersionedSource = {
              name: filename,
              packageIdentifier: packageId.identifier,
              content: this.source,
              version: packageId.identifier.version,
            }
            const schemaEdit: SchemaEdit = {
              packageIdentifier: packageId.identifier,
              edits: [
                {
                  editKind: 'CreateOrReplace',
                  sources: [source]
                } as CreateOrReplaceSource
              ],
              dryRun: false
            }
            return schemaEdit;
          }
        } as SaveQueryRequestProps,
        dismissible: true
      }
    ).subscribe(result => {
      if (result) {
        this.versionedSource = result.sourceFile;
        this.changeDetector.markForCheck();
        this.policyNeedsSaving.emit(false);
      }
    });
  }

  private saveExistingFile() {
    const versionedSource: VersionedSource = this.versionedSource
    const updatedSource: VersionedSource = {
      ...versionedSource,
      content: this.source
    }
    const schemaEdit: SchemaEdit = {
      packageIdentifier: versionedSource.packageIdentifier,
      edits: [
        {
          editKind: 'CreateOrReplace',
          sources: [updatedSource]
        } as CreateOrReplaceSource
      ],
      dryRun: false
    }
    this.schemaImporterService.submitSchemaEditOperation(schemaEdit)
      .subscribe({
        next: (result) => {
          this.alerts.open('File saved successfully', {status: 'success'})
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe()
          this.policyNeedsSaving.emit(false);
          this.changeDetector.markForCheck();
        },
        error: (error) => {
          console.error(error);
          this.alerts.open('An error occurred saving the file', {status: 'error'})
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe()
          this.changeDetector.markForCheck();
        }
      })
  }
}
