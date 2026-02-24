import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TuiDialogContext } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { UiCustomisations } from "../../environments/ui-customisations";
import { TuiInputModule } from '@taiga-ui/legacy';
import { TuiButton, TuiHint } from '@taiga-ui/core';
import { TuiAutoFocus } from '@taiga-ui/cdk';
import { HeaderComponentLayoutComponent } from '../header-component-layout/header-component-layout.component';

@Component({
  selector: 'app-test-spec-form',
  template: `
    <app-header-component-layout
      title="Download a test spec"
      [description]="description">
      <tui-input
        [(ngModel)]="testSpecName"
        [tuiHintContent]="hint"
        tuiAutoFocus
      >
        Test case name
        <input tuiTextfieldLegacy placeholder="Enter test case name"/>
      </tui-input>
      <div class="button-row">
        <button
          tuiButton
          type="button"
          size="m"
          appearance="outline-grayscale"
          (click)="onCancelClicked()"
        >
          Cancel
        </button>
        <div class="spacer"></div>
        <button
          tuiButton
          type="button"
          size="m"
          appearance="primary"
          [disabled]="!hasName"
          (click)="onDownloadClicked()"
        >
          Download
        </button>
      </div>
    </app-header-component-layout>
  `,
  styleUrls: ['./test-spec-form.component.scss'],
  standalone: true,
  imports: [
    FormsModule,
    TuiInputModule,
    TuiButton,
    TuiHint,
    TuiAutoFocus,
    HeaderComponentLayoutComponent
  ]
})
export class TestSpecFormComponent {
  protected readonly UiCustomisations = UiCustomisations;

  readonly description = `This lets you download the output of your parsed content as a test case that can be run automatically using ${UiCustomisations.productName}'s testing tools.`;
  readonly hint = 'Giving the test case a meaningful name helps explain what the test is asserting';

  testSpecName: string;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    public readonly context: TuiDialogContext<string | null>
  ) {}

  get hasName(): boolean {
    return this.testSpecName && this.testSpecName.length > 0;
  }

  onDownloadClicked() {
    this.context.completeWith(this.testSpecName);
  }

  onCancelClicked() {
    this.context.completeWith(null);
  }
}
