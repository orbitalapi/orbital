import {ChangeDetectorRef, Component, Inject} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {TuiDialogContext, TuiDialogService} from "@taiga-ui/core";
import {POLYMORPHEUS_CONTEXT} from "@tinkoff/ng-polymorpheus";

@Component({
  selector: 'app-http-endpoint-panel',
  template: `
    <app-header-component-layout title="Publish as HTTP API">
      <form [formGroup]="formGroup">
        <tui-input formControlName="endpoint" [tuiTextfieldPrefix]="prefix"
        >
          API Endpoint
          <input
            tuiTextfield
          />
        </tui-input>
        <tui-error
          formControlName="endpoint"
          [error]="[] | tuiFieldError | async"
        ></tui-error>
      </form>
      <div class="row">
        <button
          tuiButton
          type="button"
          size="m"
          appearance="outline"
          (click)="close()"
        >
          Cancel
        </button>
        <div class="spacer"></div>
        <button
          tuiButton
          type="button"
          size="m"
          appearance="primary"
          [disabled]="!formGroup.valid"
          (click)="update()"
        >
          Update
        </button>
      </div>
    </app-header-component-layout>
  `,
  styleUrls: ['./http-endpoint-panel.component.scss']
})
export class HttpEndpointPanelComponent {

  formGroup: FormGroup

  prefix = `/api/q/`
  annotationRegex = /@HttpOperation\([^)]*\)\s*/g

  constructor(@Inject(TuiDialogService) private readonly dialogs: TuiDialogService,
              @Inject(POLYMORPHEUS_CONTEXT)
              private readonly context: TuiDialogContext<string, string>,
              private changeRef: ChangeDetectorRef,) {
    this.formGroup = new FormGroup({
      endpoint: new FormControl(null, Validators.pattern('^[a-zA-Z\\-]+\$'))
    })
  }

  close() {
    this.context.completeWith(null);
  }

  update() {
    const originalQuery = this.context.data;
    const trimmed = originalQuery.replace(this.annotationRegex, '');
    const lines = trimmed.split('\n')
    const firstQueryLine = lines.findIndex(line => line.trim().startsWith('query'))
    const annotation = `@HttpOperation(url = '${this.prefix}${this.formGroup.getRawValue()['endpoint']}', method = 'GET')`


    lines.splice(firstQueryLine, 0, annotation)
    const updated = lines.join('\n')
    console.log(updated)
    this.context.completeWith(updated);
  }
}
