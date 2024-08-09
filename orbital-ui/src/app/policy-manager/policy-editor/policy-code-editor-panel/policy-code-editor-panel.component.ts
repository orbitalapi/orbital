import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  Inject,
  Injector,
} from '@angular/core';
import {ExpandingPanelSetModule} from "../../../expanding-panelset/expanding-panel-set.module";
import {AngularSplitModule} from "angular-split";
import {CodeEditorModule} from "../../../code-editor/code-editor.module";
import {CompilationMessageListModule} from "../../../compilation-message-list/compilation-message-list.module";
import {NgIf} from "@angular/common";
import {TuiAlertService, TuiButtonModule, TuiDialogService, TuiNotification} from "@taiga-ui/core";
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
    JsonViewerModule
  ],
  template: `
    <app-panel-header title="Policy editor" [isSecondary]="true">
      <div class="spacer"></div>
      <app-save-with-filename (saveFile)="saveFile()" [source]="versionedSource"></app-save-with-filename>
    </app-panel-header>
    <div class="code-editor-container">
      <app-code-editor
        [content]="source"
        [showCompilationProblemsPanel]="true"
        (contentChange)="sourceChange($event)"
      ></app-code-editor>
    </div>


    <tui-accordion [rounded]="false" [class.expanded]="authTokenOpen">
      <tui-accordion-item class="accordion-with-panel-header" [(open)]="authTokenOpen">
        <app-panel-header title="Auth Token" [isSecondary]="true">
          {{userTokenTypeName}}
        </app-panel-header>
        <ng-template tuiAccordionItemContent>
          <app-json-viewer [json]="userTokenWithTypeHints"></app-json-viewer>
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
              private changeDetector: ChangeDetectorRef) {
    policiesService.getPolicySetupReadiness().pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.policySetup = value;
      this.changeDetector.markForCheck();
    })
  }

  source: string = '';

  versionedSource: VersionedSource = null;

  sourceChange($event: string) {
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
          this.alerts.open('File saved successfully', {status: TuiNotification.Success})
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe()
          this.changeDetector.markForCheck();
        },
        error: (error) => {
          console.error(error);
          this.alerts.open('An error occurred saving the file', {status: TuiNotification.Error})
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe()
          this.changeDetector.markForCheck();
        }
      })
  }

}
