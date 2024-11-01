import {Component, Inject} from '@angular/core';
import {TuiDialogContext} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@taiga-ui/polymorpheus';
import {SharedSchemaResponse} from 'src/voyager-app/voyager.service';
import {UntypedFormControl, UntypedFormGroup} from '@angular/forms';

@Component({
  selector: 'app-share-dialog',
  styleUrls: ['./share-dialog.component.scss'],
  template: `
    <div [formGroup]="form">
      <tui-input-copy
        formControlName="shareUrl"
        tuiTextfieldSize="l"
        [readOnly]="true"
      >
        <input
          tuiTextfieldLegacy
          [readOnly]="true"
        />
        Click to copy share url
      </tui-input-copy>

    </div>
  `,
  // changeDetection: ChangeDetectionStrategy.OnPush
})
export class ShareDialogComponent {

  readonly form: UntypedFormGroup;

  constructor(@Inject(POLYMORPHEUS_CONTEXT)
              public readonly context: TuiDialogContext<void, string>,) {

    const shareUrl = context.data

    this.form = new UntypedFormGroup({
      shareUrl: new UntypedFormControl(shareUrl)
    })
  }


  get uri(): string {
    return `${window.location}${this.context.data}`
  }


}
