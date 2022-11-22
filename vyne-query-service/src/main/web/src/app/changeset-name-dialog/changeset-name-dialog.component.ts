import { Component, Inject } from '@angular/core';
import { TuiDialogContext } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { FormControl } from '@angular/forms';
import { Observable } from 'rxjs';
import { Changeset } from 'src/app/services/changeset.service';

export type ChangesetNameDialogSaveHandler = (name: string) => Observable<Changeset>;

export interface ChangesetNameDialogData {
  changeset: Changeset | null;
  saveHandler: ChangesetNameDialogSaveHandler;
}

@Component({
  selector: 'app-changeset-name-dialog',
  templateUrl: './changeset-name-dialog.component.html',
  styleUrls: ['./changeset-name-dialog.component.scss'],
})
export class ChangesetNameDialogComponent {
  nameControl!: FormControl;
  errorMessage: string | null = null;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    public context: TuiDialogContext<Changeset, ChangesetNameDialogData>,
  ) {
  }

  ngOnInit(): void {
    this.nameControl = new FormControl(this.context.data.changeset.name ?? '');
    this.nameControl.valueChanges.subscribe(() => this.errorMessage = null);
  }

  save(name: string) {
    // Merge conflict - was:
    //    this.changesetServoce
    //       .createChangeset(name, this.context.data)
    this.context.data.saveHandler(name)
      .subscribe(
        changesetResponse => this.context.completeWith(changesetResponse),
        error => {
          this.errorMessage = error.error.message
        }
      );
  }
}
