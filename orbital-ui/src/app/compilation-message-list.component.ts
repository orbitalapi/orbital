import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {CompilationMessage, groupBySource} from "./services/schema";
import {isNullOrUndefined} from "./utils/utils";

@Component({
    selector: 'app-compilation-message-list',
    template: `
      <app-panel-header [title]="title"></app-panel-header>
      <div *ngIf="!hasErrors" class="grow no-errors subtle">
        <span>There are no problems detected.</span>
      </div>
      <div class="grow" *ngIf="hasErrors">
        <tui-accordion [rounded]="false" [closeOthers]="false">
          <tui-accordion-item *ngFor="let messageGroup of compilationMessageGroups" size="s"  [open]="true">
            <div class="accordion-header">
              <img src='assets/img/tabler/align-left.svg'> {{ filenameOnly(messageGroup.source) }}
              <tui-badge [value]="messageGroup.messages.length" status="primary" size="xs"></tui-badge>
            </div>

            <div tuiAccordionItemContent>
              <div class="error-row" *ngFor="let compilationMessage of messageGroup.messages" (click)="messageClicked.emit(compilationMessage)">
                <img [attr.src]="getSeverityIcon(compilationMessage.severity)" class="filter-error-light">
                <div class="message-line">{{ compilationMessage.detailMessage }}</div>
                <div>[Ln {{ compilationMessage.line }}, Col {{ compilationMessage.char }}]</div>
              </div>
            </div>
          </tui-accordion-item>
        </tui-accordion>
      </div>

    `,
    styleUrls: ['./compilation-message-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class CompilationMessageListComponent {

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
        return source.split(']')[1]
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
