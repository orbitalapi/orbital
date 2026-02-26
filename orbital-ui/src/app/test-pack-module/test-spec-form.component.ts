import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TuiDialogContext } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { UiCustomisations } from '../../environments/ui-customisations';
import { TuiInputModule, TuiTextareaModule, TuiSelectModule } from '@taiga-ui/legacy';
import { TuiButton, TuiHint, TuiDataList } from '@taiga-ui/core';
import { TuiAutoFocus } from '@taiga-ui/cdk';
import { TuiDataListWrapper } from '@taiga-ui/kit';
import { HeaderComponentLayoutComponent } from '../header-component-layout/header-component-layout.component';
import { CommonModule } from '@angular/common';

export type RegressionPackFormat = 'Preflight' | 'Zip';

export interface TestSpecFormResult {
  name: string;
  format: RegressionPackFormat;
  description?: string;
}

interface FormatOption {
  label: string;
  value: RegressionPackFormat;
}

@Component({
  selector: 'app-test-spec-form',
  template: `
    <app-header-component-layout
      title="Download a test spec"
      [description]="description">

      <tui-select
        [(ngModel)]="selectedFormatOption"
        [stringify]="stringifyFormat"
      >
        Format
        <tui-data-list-wrapper
          *tuiDataList
          [items]="formatOptions"
          [itemContent]="formatContent"
        ></tui-data-list-wrapper>
      </tui-select>
      <ng-template #formatContent let-item>{{ item.label }}</ng-template>

      <tui-input
        [(ngModel)]="testSpecName"
        [tuiHintContent]="hint"
        tuiAutoFocus
      >
        Test case name
        <input tuiTextfieldLegacy placeholder="Enter test case name"/>
      </tui-input>

      <tui-textarea
        *ngIf="selectedFormatOption.value === 'Preflight'"
        [(ngModel)]="testDescription"
        [expandable]="true"
      >
        Description (optional)
      </tui-textarea>
      <span *ngIf="selectedFormatOption.value === 'Preflight'" class="hint">Markdown is supported</span>

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
    CommonModule,
    FormsModule,
    TuiInputModule,
    TuiTextareaModule,
    TuiSelectModule,
    TuiButton,
    TuiHint,
    TuiDataList,
    TuiAutoFocus,
    TuiDataListWrapper,
    HeaderComponentLayoutComponent
  ]
})
export class TestSpecFormComponent {
  protected readonly UiCustomisations = UiCustomisations;

  readonly description = `This lets you download the output of your parsed content as a test case that can be run automatically using ${UiCustomisations.productName}'s testing tools.`;
  readonly hint = 'Giving the test case a meaningful name helps explain what the test is asserting';

  readonly formatOptions: FormatOption[] = [
    { label: 'Preflight test spec (recommended)', value: 'Preflight' },
    { label: 'Zip file', value: 'Zip' }
  ];

  selectedFormatOption: FormatOption = this.formatOptions[0];
  stringifyFormat = (option: FormatOption) => option.label;
  testSpecName: string;
  testDescription: string;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    public readonly context: TuiDialogContext<TestSpecFormResult | null>
  ) {}

  get hasName(): boolean {
    return this.testSpecName && this.testSpecName.length > 0;
  }

  onDownloadClicked() {
    this.context.completeWith({
      name: this.testSpecName,
      format: this.selectedFormatOption.value,
      description: this.testDescription || undefined
    });
  }

  onCancelClicked() {
    this.context.completeWith(null);
  }
}
