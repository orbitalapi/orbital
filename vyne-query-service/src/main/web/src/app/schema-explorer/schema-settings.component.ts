import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { SourcePackageDescription } from 'src/app/package-viewer/packages.service';
import { SchemaImporterService } from 'src/app/schema-importer/schema-importer.service';
import { Message } from 'src/app/services/schema';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-schema-settings',
  template: `
    <app-git-config *ngIf="packageDescription.publisherType === 'GitRepo'" [editable]="false"
                    [gitConfig]="packageDescription.packageConfig"></app-git-config>
    <app-file-config *ngIf="packageDescription.publisherType === 'FileSystem'" [editable]="false"
                     [fileSystemPackageConfig]="packageDescription.packageConfig"></app-file-config>

    <ng-container *ngIf="canRemove">
      <hr>
      <h3>Danger zone</h3>
      <button tuiButton appearance="secondary-destructive" [showLoader]="working" (click)="removeRepository()">
        Remove this schema
      </button>
      <tui-notification [status]="deleteResultMessage.level.toLowerCase()" *ngIf="deleteResultMessage">
        {{ deleteResultMessage.message }}
      </tui-notification>
    </ng-container>

  `,
  styleUrls: ['./schema-settings.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SchemaSettingsComponent {

  @Input()
  packageDescription: SourcePackageDescription;
  working = false;
  deleteResultMessage: Message;

  constructor(private changeDetector: ChangeDetectorRef, private service: SchemaImporterService,
              private router: Router, private snackbar: MatSnackBar,
              private activeRoute: ActivatedRoute
    ,) {
  }

  get canRemove() {
    return !this.packageDescription.identifier.id.startsWith('io.vyne/core-types');
    // return this.packageDescription.publisherType !== 'Pushed';
  }

  removeRepository() {
    this.working = true;
    this.service.removeRepository(this.packageDescription)
      .subscribe(result => {
          this.snackbar.open('Schema was successfully removed', 'Dismiss', {
            duration: 5000,
          });
          this.working = false;
          this.router.navigate(['..'], {
            relativeTo: this.activeRoute
          });
        },
        error => {
          this.deleteResultMessage = {
            message: 'A problem occurred removing the schema',
            level: 'ERROR',
          }
          this.working = false;
          this.changeDetector.markForCheck();
        })
    this.changeDetector.markForCheck();
  }

}
