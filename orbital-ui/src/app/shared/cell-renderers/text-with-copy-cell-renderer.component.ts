import { Component, Inject } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TuiHintOverflow, TuiButton, TuiAlertService } from '@taiga-ui/core';
import { CommonModule } from '@angular/common';
import { Clipboard } from '@angular/cdk/clipboard';

@Component({
  selector: 'app-text-with-copy-cell-renderer',
  standalone: true,
  imports: [
    CommonModule,
    TuiHintOverflow,
    TuiButton
  ],
  template: `
    <div class="cell-container">
      <span class="cell-text" tuiHintOverflow tuiHintAppearance="dark">
        {{ value }}
      </span>
      <button 
        class="copy-button" 
        tuiIconButton 
        appearance="flat" 
        size="xs"
        (click)="copyToClipboard()" 
        title="Click to copy to clipboard">
        <img src="assets/img/tabler/clipboard.svg" alt="Copy">
      </button>
    </div>
  `,
  styleUrl: './text-with-copy-cell-renderer.component.scss'
})
export class TextWithCopyCellRendererComponent implements ICellRendererAngularComp {
  public value: string = '';
  
  constructor(
    private clipboard: Clipboard,
    @Inject(TuiAlertService) private readonly alerts: TuiAlertService
  ) {}

  agInit(params: ICellRendererParams): void {
    this.value = params.value || '';
  }

  refresh(params: ICellRendererParams): boolean {
    this.value = params.value || '';
    return true;
  }

  copyToClipboard(): void {
    this.clipboard.copy(this.value);
    this.alerts.open('Copied to clipboard', { appearance: 'success' })
      .subscribe();
  }
}