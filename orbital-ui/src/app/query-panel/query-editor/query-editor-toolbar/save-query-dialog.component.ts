import {ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, Inject, Input} from '@angular/core';
import {PackagesService, SourcePackageDescription} from "../../../package-viewer/packages.service";
import {
  AbstractControl,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {TUI_VALIDATION_ERRORS} from "@taiga-ui/kit";
import {TuiAlertService, TuiDialogContext, TuiNotification} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from "@tinkoff/ng-polymorpheus";
import {
  CreateOrReplaceQuery,
  SavedQueryWithSource,
  SchemaEdit,
  SchemaImporterService
} from '../../../project-import/schema-importer.service';
import {VersionedSource} from '../../../services/schema';
import {schema} from "@angular-devkit/core";

export interface SaveQueryRequestProps {
  query: string,
  previousVersion?: SavedQueryWithSource,
  existingSavedQueryNames: string[],
  schemaEditBuilder?:  (packageId: SourcePackageDescription, filename: string) => SchemaEdit
}

@Component({
  selector: 'app-save-query-dialog',
  template: `
    <app-header-component-layout title="Save query">
      <tui-notification *ngIf="!hasEditablePackages"
                        status="error"
      >
        You don't currently have any projects that are editable. Add or configure a project in the <a
        [routerLink]="['/schemas']" (click)='close()'>Schemas</a> view
      </tui-notification>
      <form [formGroup]="formGroup">
        <app-project-selector formControlName="schemaPackage"
                              [disabled]="!hasEditablePackages"
                              [packages]="editablePackages"
                              prompt="Select a project to save the query to"
        >
        </app-project-selector>
        <tui-error
          formControlName="schemaPackage"
          [error]="[] | tuiFieldError | async"
        ></tui-error>
        <tui-input formControlName="queryName"
        >
          Query name
          <input [disableControl]="!hasEditablePackages"
                 tuiTextfield
          />
          <span class="tui-required"></span>
        </tui-input>
        <tui-error
          formControlName="queryName"
          [error]="[] | tuiFieldError | async"
        ></tui-error>
      </form>
      <tui-notification *ngIf="errorMessage" [status]="'error'">{{ errorMessage }}</tui-notification>
      <div class="row">
        <button
          tuiButton
          type="button"
          size="m"
          appearance="outline"
          (click)="close()"
        >
          Cancel
        </button>
        <div class="spacer"></div>
        <button
          tuiButton
          type="button"
          size="m"
          appearance="primary"
          [disabled]="!formGroup.valid"
          (click)="save()"
        >
          Save
        </button>
      </div>

    </app-header-component-layout>
  `,
  styleUrls: ['./save-query-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: TUI_VALIDATION_ERRORS,
      useValue: {
        required: 'This is required',
        pattern: 'Names must start with a letter, and contain letters, numbers and underscores only.',
        isExisting: 'A query with that name already exists',
      },
    },
  ],
})
export class SaveQueryDialogComponent {
  readonly stringify = (item: SourcePackageDescription) => item.identifier.name;

  @Input()
  packages: SourcePackageDescription[];

  formGroup: UntypedFormGroup

  private readonly alerts = inject(TuiAlertService);

  constructor(private packagesService: PackagesService,
              private schemaImporterService: SchemaImporterService,
              @Inject(POLYMORPHEUS_CONTEXT)
              private readonly context: TuiDialogContext<SavedQueryWithSource, SaveQueryRequestProps>,
              private changeRef: ChangeDetectorRef,
  ) {
    this.formGroup = new UntypedFormGroup({
      schemaPackage: new UntypedFormControl(null, Validators.required),
      queryName: new UntypedFormControl(null,
        [Validators.required, Validators.pattern('[a-zA-Z](\\w|\\d)*'), this.existingQueryNameValidator()])
    })
    this.packagesService.listPackages()
      .subscribe(result => {
        this.packages = result;
        changeRef.markForCheck();
      });
  }

  get hasEditablePackages(): boolean {
    return this.editablePackages.length > 0;
  }

  get editablePackages(): SourcePackageDescription[] {
    if (!this.packages) {
      return [];
    }
    return this.packages.filter(p => p.editable);
  }

  errorMessage: string;

  close() {
    this.context.completeWith(null);
  }

  save() {
    const formData = this.formGroup.getRawValue() as { schemaPackage: SourcePackageDescription, queryName: string }
    const fileName = formData.queryName + '.taxi'
    const schemaEdit: SchemaEdit = this.buildSchemaEdit(formData.schemaPackage, fileName)

    this.changeRef.markForCheck();
    this.schemaImporterService.submitSchemaEditOperation(schemaEdit)
      .subscribe({
        next: (result) => {
          this.alerts.open('Query saved successfully', {status: TuiNotification.Success})
            //.pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe()

          const updatedState = this.schemaImporterService.getQueryStateFromEditResult(result, fileName)
          this.context.completeWith(updatedState);
        },
        error: (error) => {
          console.error(error);
          this.errorMessage = error.error?.message || error.message;
          this.changeRef.markForCheck();
        }
      })
  }

  private existingQueryNameValidator(): ValidatorFn {
    return (control:AbstractControl) : ValidationErrors | null => {
      if (!control.value) {
        return null;
      }
      const valueExists = this.context.data.existingSavedQueryNames.includes(control.value)
      return valueExists ? {isExisting: true}: null;
    }
  }

  private buildSchemaEdit(schemaPackage: SourcePackageDescription, fileName: string):SchemaEdit {
    if (this.context.data.schemaEditBuilder) {
      return this.context.data.schemaEditBuilder(schemaPackage, fileName)
    } else {
      const source: VersionedSource = {
        name: fileName,
        packageIdentifier: schemaPackage.identifier,
        content: this.context.data.query,
        version: schemaPackage.identifier.version,
      }
      const schemaEdit: SchemaEdit = {
        packageIdentifier: schemaPackage.identifier,
        edits: [
          {
            editKind: 'CreateOrReplaceQuery',
            sources: [source]
          } as CreateOrReplaceQuery
        ],
        dryRun: false
      }
      return schemaEdit
    }
  }
}
