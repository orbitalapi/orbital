import { ChangeDetectionStrategy, Component, Inject, OnInit } from '@angular/core';
import { TuiDialogContext } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { SharedSchemaResponse } from 'src/taxi-playground-app/taxi-playground.service';
import { FormControl, FormGroup } from '@angular/forms';

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

  readonly form: FormGroup;

  constructor(@Inject(POLYMORPHEUS_CONTEXT)
              public readonly context: TuiDialogContext<void, SharedSchemaResponse>,) {

    const href = window.location.origin;
    const windowLocation = href.endsWith('/') ? href.slice(0, -1) : href
    const shareUrl = windowLocation + context.data.uri;

    this.form = new FormGroup({
      shareUrl: new FormControl(shareUrl)
    })
  }


  get uri(): string {
    return `${window.location}${this.context.data.uri}`
  }


}
