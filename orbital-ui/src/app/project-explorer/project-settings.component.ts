import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input } from '@angular/core';
import { SourcePackageDescription } from 'src/app/package-viewer/packages.service';
import { SchemaImporterService } from 'src/app/project-import/schema-importer.service';
import { Message } from 'src/app/services/schema';
import { TuiAlertService, TuiDialogService } from '@taiga-ui/core';
import { TUI_PROMPT } from '@taiga-ui/kit';

@Component({
  selector: 'app-project-settings',
  template: `
      <app-git-config *ngIf="packageDescription.publisherType === 'GitRepo'" [editable]="false"
                      [gitConfig]="packageDescription.packageConfig"></app-git-config>
      <app-file-config *ngIf="packageDescription.publisherType === 'FileSystem'" [editable]="false"
                       [fileSystemPackageConfig]="packageDescription.packageConfig"></app-file-config>

      <ng-container *ngIf="canRemove">
          <hr>
          <h3>Danger zone</h3>
          <button tuiButton appearance="secondary-destructive" [showLoader]="working" (click)="confirmRemoval()">
              Remove this project...
          </button>
          <tui-notification [status]="deleteResultMessage.severity.toLowerCase()" *ngIf="deleteResultMessage">
              {{ deleteResultMessage.message }}
          </tui-notification>
      </ng-container>

  `,
  styleUrls: ['./project-settings.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectSettingsComponent {

  @Input()
  packageDescription: SourcePackageDescription;
  working = false;
  deleteResultMessage: Message;

  constructor(
    private changeDetector: ChangeDetectorRef,
    private service: SchemaImporterService,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    @Inject(TuiAlertService) private readonly alertService: TuiAlertService
  ) {
  }

  get canRemove() {
    return !this.packageDescription.identifier.id.startsWith('io.vyne/core-types');
    // return this.packageDescription.publisherType !== 'Pushed';
  }

  confirmRemoval() {
    this.dialogService
      .open<boolean>(TUI_PROMPT, {
        label: 'Are you sure?',
        data: {
          content: `When you remove a project, any data sources and data types within the project are also removed.` +
            ` As a result, queries might stop working, and data may become unavailable.</br>` +
            `The project is removed from your workspace, but isn't deleted from ${this.packageDescription.publisherType === 'GitRepo' ? 'git' : 'disk'},` +
            ` so you can always add it again later.`,
          yes: 'Remove',
          no: 'Cancel',
        },
      })
      .subscribe(response => {
        if (response) this. removeRepository();
      });
  }

  removeRepository() {
    this.working = true;
    this.service.removeRepository(this.packageDescription)
      .subscribe({
        next: result => {
          this.alertService.open('Project was successfully removed', {status: 'success', autoClose: 5000 })
            .subscribe()
          this.working = false;
        },
        error: () => {
          this.deleteResultMessage = {
            message: 'A problem occurred removing the project',
            severity: 'ERROR',
          }
          this.working = false;
          this.changeDetector.markForCheck();
        }
      })
    this.changeDetector.markForCheck();
  }

}
