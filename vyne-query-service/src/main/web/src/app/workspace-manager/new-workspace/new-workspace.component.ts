import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {TUI_VALIDATION_ERRORS} from "@taiga-ui/kit";
import {WorkspacesService} from "../../services/workspaces.service";

@Component({
    selector: 'app-new-workspace',
    styleUrls: ['./new-workspace.component.scss'],
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
    template: `
        <app-header-bar>
        </app-header-bar>
        <app-header-component-layout title='New workspace'
                                     description="A workspace allows you to configure a group of data sources and schemas"
        >

            <form [formGroup]="formGroup">
                <div class="form-container">
                    <div class="form-body">
                        <div class="form-row">
                            <div class="form-item-description-container">
                                <h3>Workspace name</h3>
                                <div class="help-text">
                                    <p>
                                        Give your workspace a name. Great names are short and memorable.
                                    </p>
                                </div>
                            </div>
                            <div class="form-element">
                                <div class="row">
                                    <tui-input formControlName="workspaceName"
                                               class="flex-grow">
                                        Workspace name
                                    </tui-input>
                                </div>
                                <tui-error class="row"
                                           formControlName="workspaceName"
                                           [error]="[] | tuiFieldError | async"
                                ></tui-error>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
            <tui-notification *ngIf="errorMessage" [status]="'error'">{{ errorMessage }}</tui-notification>
            <div class="row">
                <div class="spacer"></div>
                <button
                        tuiButton
                        type="button"
                        size="m"
                        appearance="primary"
                        [disabled]="!formGroup.valid"
                        (click)="save()"
                        [showLoader]="working"
                >
                    Save
                </button>
            </div>
        </app-header-component-layout>
    `
})
export class NewWorkspaceComponent {
    formGroup: FormGroup;
    working: boolean = false;

    errorMessage: string | null = null;

    constructor(private workspaceService: WorkspacesService,
                private changeRef: ChangeDetectorRef
    ) {
        this.formGroup = new FormGroup({
            workspaceName: new FormControl(null,
                [Validators.required, Validators.pattern('[a-zA-Z](\\w|\\d)*')]
            )
        })
    }

    save() {
        this.errorMessage = null;
        this.working = true;
        const formData = this.formGroup.getRawValue() as { workspaceName: string }
        this.workspaceService.createWorkspace(formData.workspaceName)
            .subscribe(value => {
                    console.log('Workspace created')
                    this.working = false
                    this.changeRef.markForCheck()
                },
                error => {
                    this.errorMessage = error.message;
                    this.working = false
                    this.changeRef.markForCheck();
                },
                () => {

                })
    }

}
