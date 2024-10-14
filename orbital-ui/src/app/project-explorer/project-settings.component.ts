import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  Inject,
  input,
} from '@angular/core';
import { SourcePackageDescription } from 'src/app/package-viewer/packages.service';
import { SchemaImporterService } from 'src/app/project-import/schema-importer.service';
import { Message } from 'src/app/services/schema';
import { TuiAlertService, TuiDialogService, TuiButtonModule } from '@taiga-ui/core';
import { TUI_PROMPT } from '@taiga-ui/kit';
import {Router} from "@angular/router";
import { TuiNotificationModule } from '@taiga-ui/core/components/notification';
import {CommonModule} from '@angular/common';
import {FileConfigComponent} from '../project-import/project-source-config/file-config.component';
import {GitConfigComponent} from '../project-import/project-source-config/git-config.component';

@Component({
    selector: 'app-project-settings',
    template: `
      <app-git-config *ngIf="packageDescription()?.publisherType === 'GitRepo'" [editable]="false"
                      [gitConfig]="packageDescription().packageConfig"></app-git-config>
      <app-file-config *ngIf="packageDescription()?.publisherType === 'FileSystem'" [editable]="false"
                       [fileSystemPackageConfig]="packageDescription().packageConfig"></app-file-config>
      <ng-container *ngIf="canRemove()">
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
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
  imports: [CommonModule, TuiButtonModule, TuiNotificationModule, GitConfigComponent, FileConfigComponent],
})
export class ProjectSettingsComponent {

  packageDescription = input<SourcePackageDescription>();
  working = false;
  deleteResultMessage: Message;

  constructor(
    private changeDetector: ChangeDetectorRef,
    private service: SchemaImporterService,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    @Inject(TuiAlertService) private readonly alertService: TuiAlertService,
    private router: Router
  ) {
  }

  canRemove = computed(() => {
    return !this.packageDescription()?.identifier.id.startsWith('com.orbitalhq/core-types');
    // return this.packageDescription.publisherType !== 'Pushed';
  })

  confirmRemoval() {
    this.dialogService
      .open<boolean>(TUI_PROMPT, {
        label: 'Are you sure?',
        data: {
          content: `When you remove a project, any data sources and data types within the project are also removed.` +
            ` As a result, queries might stop working, and data may become unavailable.</br>` +
            `The project is removed from your workspace, but isn't deleted from ${this.packageDescription().publisherType === 'GitRepo' ? 'git' : 'disk'},` +
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
    this.service.removeRepository(this.packageDescription())
      .subscribe({
        next: result => {
          this.alertService.open('Project was successfully removed', {status: 'success', autoClose: 5000 })
            .subscribe()
          this.working = false;
          this.changeDetector.markForCheck();
          this.router.navigate(['projects'])
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
