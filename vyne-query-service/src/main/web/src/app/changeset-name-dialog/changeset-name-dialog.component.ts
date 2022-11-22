import { Component, Inject } from '@angular/core';
import { TuiDialogContext } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { FormControl } from '@angular/forms';
import { Observable } from 'rxjs';

export type ChangesetNameDialogSaveHandler = (name: string) => Observable<void>;

export interface ChangesetNameDialogData {
  name: string | null;
  saveHandler: ChangesetNameDialogSaveHandler;
}

@Component({
  selector: 'app-changeset-name-dialog',
  templateUrl: './changeset-name-dialog.component.html',
  styleUrls: ['./changeset-name-dialog.component.scss'],
})
export class ChangesetNameDialogComponent {
  nameControl!: FormControl;
  hasError = false;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    public context: TuiDialogContext<string, ChangesetNameDialogData>,
  ) {
  }

  ngOnInit(): void {
    this.nameControl = new FormControl(this.context.data.name ?? '');
    this.nameControl.valueChanges.subscribe(() => this.hasError = false);
  }

  save(name: string) {
    this.context.data.saveHandler(name)
      .subscribe(
        () => this.context.completeWith(name),
        error => this.hasError = true, // TODO Deduce what was wrong instead of defaulting to "name already exists"
      );
  }
}
