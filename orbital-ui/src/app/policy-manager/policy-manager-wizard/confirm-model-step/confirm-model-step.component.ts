import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TuiButtonModule, TuiNotificationModule } from '@taiga-ui/core';
import { CodeEditorModule } from '../../../code-editor/code-editor.module';
import { SourcePackageDescription } from '../../../package-viewer/packages.service';
import {
  CreateOrReplaceSource,
  SchemaEdit,
  SchemaImporterService
} from '../../../project-import/schema-importer.service';
import { Message } from '../../../services/schema';

@Component({
  selector: 'app-confirm-model-step',
  standalone: true,
  imports: [CommonModule, CodeEditorModule, TuiButtonModule, TuiNotificationModule],
  template: `
    <h4>Confirm model</h4>
    <p>
      A taxi model has been generated based on the fields you selected from your authentication token.
    </p>
    <p>
      You can make any required changes below before continuing.
    </p>
    <tui-notification
      [status]="schemaSaveResultMessage.level.toLowerCase()"
      *ngIf="schemaSaveResultMessage"
      class="notification"
      (close)="schemaSaveResultMessage = null"
    >
      {{ schemaSaveResultMessage.message }}
    </tui-notification>
    <app-code-editor class="code-editor" [(content)]="pendingEdits.sources[0].content"></app-code-editor>
    <div class="form-button-bar">
      <button tuiButton appearance="secondary" [size]="'m'" (click)="gotoMapAuthTokenStep.emit()">Back</button>
      <button tuiButton [showLoader]="working" [size]="'m'" (click)="onSubmit()">Confirm</button>
    </div>
  `,
  styleUrls: ['./confirm-model-step.component.scss']
})
export class ConfirmModelStepComponent {
  @Input()
  pendingEdits!: CreateOrReplaceSource;

  @Input() selectedProject!: SourcePackageDescription;

  @Output()
  gotoWritePolicyStep: EventEmitter<void> = new EventEmitter<void>()

  @Output()
  gotoMapAuthTokenStep: EventEmitter<void> = new EventEmitter<void>()

  working: boolean;
  schemaSaveResultMessage: Message;

  constructor(
    private schemaService: SchemaImporterService,
    private changeDetectorRef: ChangeDetectorRef,
  ) {}

  onSubmit() {
    const schemaEdit: SchemaEdit = {
      packageIdentifier: this.selectedProject.identifier,
      edits: [this.pendingEdits],
      dryRun: false
    }

    this.working = true;
    this.schemaService.submitSchemaEditOperation(schemaEdit)
      .subscribe(() => {
          this.gotoWritePolicyStep.emit();
        },
        error => {
          console.error(JSON.stringify(error));
          this.schemaSaveResultMessage = {
            message: error.error?.message || 'An error occurred',
            level: 'ERROR',
          };
          this.working = false;
          this.changeDetectorRef.markForCheck();
        },
      );
  }

}
