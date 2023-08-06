import {Component, Inject, OnInit} from '@angular/core';
import {TuiDialogContext} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';
import {FormControl} from '@angular/forms';
import {Changeset} from 'src/app/changeset-selector/changeset.service';
import {ChangesetNameDialogSaveHandler} from "./changeset-name-dialog-save.handler";

export interface ChangesetNameDialogData {
  changeset: Changeset | null;
  saveHandler: ChangesetNameDialogSaveHandler;
}

@Component({
  selector: 'app-changeset-name-dialog',
  templateUrl: './changeset-name-dialog.component.html',
  styleUrls: ['./changeset-name-dialog.component.scss'],
})
export class ChangesetNameDialogComponent implements OnInit {
  nameControl!: FormControl;
  errorMessage: string | null = null;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    public context: TuiDialogContext<Changeset, ChangesetNameDialogData>,
  ) {
  }

  ngOnInit(): void {
    const changeset = this.context.data.changeset;
    this.nameControl = new FormControl(changeset.isDefault ? '' : changeset.name);
    this.nameControl.valueChanges.subscribe(() => this.errorMessage = null);
  }

  save(name: string) {
    this.context.data.saveHandler(name, this.context.data.changeset.packageIdentifier)
      .subscribe(
        changesetResponse => this.context.completeWith(changesetResponse),
        error => {
          this.errorMessage = error.error.message
        }
      );
  }
}
