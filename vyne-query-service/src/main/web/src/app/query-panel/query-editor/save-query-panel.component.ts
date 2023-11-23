import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input} from '@angular/core';
import {PackagesService, SourcePackageDescription} from "../../package-viewer/packages.service";
import {UntypedFormControl, UntypedFormGroup, Validators} from "@angular/forms";
import {TUI_VALIDATION_ERRORS} from "@taiga-ui/kit";
import {TuiDialogContext, TuiDialogService} from "@taiga-ui/core";
import {POLYMORPHEUS_CONTEXT} from "@tinkoff/ng-polymorpheus";
import {SavedQuery, TypeEditorService} from "../../services/type-editor.service";
import {MatLegacySnackBar as MatSnackBar} from "@angular/material/legacy-snack-bar";

export interface SaveQueryPanelProps {
  query: string,
  previousVersion?: SavedQuery
}

@Component({
  selector: 'app-save-query-panel',
  template: `
    <app-header-component-layout title="Save query">
      <tui-notification *ngIf="!hasEditablePackages"
                        status="error"
      >
        You don't currently have any projects that are editable. Add or configure a project in the <a
        [routerLink]="['schemas']">Schemas</a> view
      </tui-notification>
      <form [formGroup]="formGroup">


        <app-project-selector formControlName="schemaPackage"
                              [disabled]="!hasEditablePackages"
                              [packages]="editablePackages"
                              prompt="Select a project to save the query to"
        >


        </app-project-selector>
        <!--              <tui-select-->
        <!--                      [stringify]="stringify"-->
        <!--                      formControlName="schemaPackage"-->

        <!--              >-->
        <!--                  Select a project to save the query to-->
        <!--                  <input-->
        <!--                          tuiTextfield [disableControl]="!hasEditablePackages"-->
        <!--                  />-->
        <!--                  <tui-data-list-wrapper-->
        <!--                          *tuiDataList-->
        <!--                          [items]="editablePackages | tuiFilterByInputWith : stringify"-->
        <!--                          [itemContent]="stringify | tuiStringifyContent"-->
        <!--                  ></tui-data-list-wrapper>-->
        <!--              </tui-select>-->
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
  styleUrls: ['./save-query-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: TUI_VALIDATION_ERRORS,
      useValue: {
        required: 'This is required',
        pattern: 'Names must start with a letter, and contain letters, numbers and underscores only.',
      },
    },
  ],
})
export class SaveQueryPanelComponent {
  readonly stringify = (item: SourcePackageDescription) => item.identifier.name;

  @Input()
  packages: SourcePackageDescription[];

  formGroup: UntypedFormGroup

  working: boolean = false;

  constructor(private packagesService: PackagesService,
              private editorService: TypeEditorService,
              @Inject(TuiDialogService) private readonly dialogs: TuiDialogService,
              @Inject(POLYMORPHEUS_CONTEXT)
              private readonly context: TuiDialogContext<SavedQuery, SaveQueryPanelProps>,
              private changeRef: ChangeDetectorRef,
              private snackBar: MatSnackBar
  ) {
    this.formGroup = new UntypedFormGroup({
      schemaPackage: new UntypedFormControl(null, Validators.required),
      queryName: new UntypedFormControl(null,
        [Validators.required, Validators.pattern('[a-zA-Z](\\w|\\d)*')])
    })
    this.packagesService.listPackages()
      .subscribe(result => this.packages = result);
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
    this.working = true;
    this.changeRef.markForCheck();
    this.editorService.saveQuery({
      source: {
        name: formData.queryName + '.taxi',
        packageIdentifier: formData.schemaPackage.identifier,
        content: this.context.data.query,
        version: formData.schemaPackage.identifier.version,
      },
      changesetName: '' // TODO ... add changesets across this stuff.
    })
      .subscribe(result => {
        this.snackBar.open('Query saved successfully');
        this.context.completeWith(result);
      }, error => {
        console.error(error);
        this.working = false;
        this.errorMessage = error.message;
        this.changeRef.markForCheck();
      })
  }
}
