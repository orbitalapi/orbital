import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {CompilationMessage, groupBySource} from "./services/schema";
import {isNullOrUndefined} from "./utils/utils";

@Component({
  selector: 'app-compilation-message-list',
  template: `
    <app-panel-header [collapsible]="true" [title]="title" [isSecondary]="true"
                      [class.has-errors]="hasErrors" [(expanded)]="expanded"></app-panel-header>
    <tui-expand [expanded]="expanded">
      <div *ngIf="!hasErrors" class="grow no-errors subtle">
        <span>There are no problems detected.</span>
      </div>
      <div class="grow" *ngIf="hasErrors">
        <tui-accordion [rounded]="false" [closeOthers]="false">
          <tui-accordion-item *ngFor="let messageGroup of compilationMessageGroups" size="s" [open]="true">
            <div class="accordion-header">
              <img src='assets/img/tabler/align-left.svg'> {{ filenameOnly(messageGroup.source) }}
              <tui-badge-notification size="s">{{ messageGroup.messages.length }}</tui-badge-notification>
            </div>
            <div tuiAccordionItemContent>
              <div class="error-row" *ngFor="let compilationMessage of messageGroup.messages"
                   (click)="messageClicked.emit(compilationMessage)">
                <img [attr.src]="getSeverityIcon(compilationMessage.severity)"
                     [class]="compilationMessage.severity === 'ERROR' ? 'filter-error-light' : 'filter-warning-dark'">
                <div class="message-line">{{ compilationMessage.detailMessage }}</div>
                <div class="message-position">[Ln {{ compilationMessage.line }}, Col {{ compilationMessage.char }}]
                </div>
              </div>
            </div>
          </tui-accordion-item>
        </tui-accordion>
      </div>
    </tui-expand>
  `,
  styleUrls: ['./compilation-message-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CompilationMessageListComponent {
  private _expanded = true

  @Input()
  get expanded(): boolean {
    return this._expanded;
  }

  set expanded(value: boolean) {
    this._expanded = value;
    this.expandedChange.emit(this.expanded);
  }

  @Output()
  expandedChange = new EventEmitter<boolean>()

  compilationMessageGroups: CompilationMessageGroup[] = []






  private _compilationMessages: CompilationMessage[];

  @Output()
  messageClicked = new EventEmitter<CompilationMessage>();

  @Input()
  get compilationMessages(): CompilationMessage[] {
    return this._compilationMessages;
  }

  set compilationMessages(value: CompilationMessage[]) {
    this._compilationMessages = value;
    this.compilationMessageGroups = this.buildCompilationMessageGroups()
  }

  get hasErrors(): boolean {
    return this._compilationMessages?.length > 0 || false;
  }

  get title(): string {
    return this.hasErrors ? `Problems (${this._compilationMessages.length})` : 'Problems';
  }

  private buildCompilationMessageGroups(): CompilationMessageGroup[] {
    if (isNullOrUndefined(this._compilationMessages)) {
      return [];
    }
    const messagesBySource = Array.from(groupBySource(this._compilationMessages).entries())
      .map(([source, messages]) => {
        return {
          source,
          messages
        } as CompilationMessageGroup
      })
    return messagesBySource;
  }


  filenameOnly(source: string) {
    const filename = source.split(']')[1];
    return filename?.includes('unknown') ? '' : filename;
  }

  getSeverityIcon(severity: "INFO" | "WARNING" | "ERROR"): string {
    const path = `assets/img/tabler`;
    switch (severity) {
      case "ERROR":
        return `${path}/circle-x.svg`;
      case "WARNING":
        return `${path}/alert-triangle.svg`;
      case "INFO":
        return `${path}/info-circle.svg`
    }
  }
}

export interface CompilationMessageGroup {
  source: string;
  messages: CompilationMessage[]
}
