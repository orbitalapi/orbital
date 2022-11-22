import { Component, Inject, OnInit } from '@angular/core';
import { TuiDialogContext } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { FormControl } from '@angular/forms';
import { map } from 'rxjs/operators';
import { Changeset, ChangesetService } from 'src/app/services/changeset.service';
import { PackageIdentifier } from 'src/app/package-viewer/packages.service';

@Component({
  selector: 'app-changeset-name-dialog',
  templateUrl: './changeset-name-dialog.component.html',
  styleUrls: ['./changeset-name-dialog.component.scss']
})
export class ChangesetNameDialogComponent {

  nameControl = new FormControl();
  errorMessage: string | null = null;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    public context: TuiDialogContext<Changeset, PackageIdentifier>,
    private changesetServoce: ChangesetService,
  ) {
  }

  ngOnInit(): void {
    this.nameControl.valueChanges.subscribe(() => this.errorMessage = null);
  }

  save(name: string) {
    this.changesetServoce
      .createChangeset(name, this.context.data)
      .subscribe(
        changesetResponse => this.context.completeWith(changesetResponse.changeset),
        error => {
          this.errorMessage = error.error.message
        }
      );
  }
}
