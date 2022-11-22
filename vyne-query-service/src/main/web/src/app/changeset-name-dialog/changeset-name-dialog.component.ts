import { Component, Inject, OnInit } from '@angular/core';
import { TuiDialogContext } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { FormControl } from '@angular/forms';
import { map } from 'rxjs/operators';
import { fakePackageIdentifier, TypesService } from '../services/types.service';

@Component({
  selector: 'app-changeset-name-dialog',
  templateUrl: './changeset-name-dialog.component.html',
  styleUrls: ['./changeset-name-dialog.component.scss']
})
export class ChangesetNameDialogComponent {

  nameControl = new FormControl();
  hasError = false;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    public context: TuiDialogContext<string, void>,
    private typesService: TypesService,
    ) { }

  ngOnInit(): void {
    this.nameControl.valueChanges.subscribe(() => this.hasError = false);
  }

  save(name: string) {
    this.typesService
      .createChangeset(name)
      .subscribe(
        () => this.context.completeWith(name),
        error => this.hasError = true // TODO Deduce what was wrong instead of defaulting to "name already exists"
      );
  }
}
