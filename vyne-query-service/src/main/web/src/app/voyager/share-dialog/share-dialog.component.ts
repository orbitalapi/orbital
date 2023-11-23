import {Component, Inject} from '@angular/core';
import {TuiDialogContext} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';
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
          tuiTextfield
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
              public readonly context: TuiDialogContext<void, SharedSchemaResponse>,) {

    const href = window.location.origin;
    const windowLocation = href.endsWith('/') ? href.slice(0, -1) : href
    const shareUrl = windowLocation + context.data.uri;

    this.form = new UntypedFormGroup({
      shareUrl: new UntypedFormControl(shareUrl)
    })
  }


  get uri(): string {
    return `${window.location}${this.context.data.uri}`
  }


}
