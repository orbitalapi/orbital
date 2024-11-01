import {TuiAutoFocus} from '@taiga-ui/cdk';
import { TUI_CONFIRM } from "@taiga-ui/kit";
import { TuiTextareaModule} from '@taiga-ui/legacy';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  output,
  model,
  effect,
  ViewChild,
  Inject,
} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Clipboard} from '@angular/cdk/clipboard';
import { TuiAlertService, TuiDialogService, TuiLink, TuiButton, TuiHint } from '@taiga-ui/core';
import {MarkdownComponent} from 'ngx-markdown';
import {ExpandingPanelSetModule} from '../../../expanding-panelset/expanding-panel-set.module';
import {ConversationMessage} from '../../../services/query.service';
import {QueryState} from '../query-editor-toolbar/query-editor-toolbar.component';

@Component({
  selector: 'app-copilot-panel',
  standalone: true,
  imports: [
    CommonModule,
    ExpandingPanelSetModule,
    TuiButton,
    FormsModule,
    MarkdownComponent,
    TuiHint,
    TuiLink,
    TuiTextareaModule,
    TuiAutoFocus
  ],
  templateUrl: './copilot-panel.component.html',
  styleUrl: './copilot-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CopilotPanelComponent {
  @ViewChild('results') private results: ElementRef;

  chatQuery = model<string>()
  conversationMessages = input<ConversationMessage[]>()
  currentState = input<QueryState>()

  runQuery = output<string>();
  setQueryInEditor = output<string>();
  submitTextToChatGpt = output<string>();
  deleteChatHistory = output<void>();
  close = output<void>();

  constructor(
    public clipboard: Clipboard,
    @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
  ) {
    effect(() => {
      if (this.conversationMessages()?.length) {
        this.scrollToBottom()
      }
    });
  }

  submitChat(event?: KeyboardEvent) {
    event?.preventDefault();
    this.submitTextToChatGpt.emit(this.chatQuery())
  }

  findQueryToRun(index?: number) {
    const query = this.findQuery(index);
    this.runQuery.emit(query)
  }

  findQueryToSetInEditor(index?: number) {
    const query = this.findQuery(index);
    this.setQueryInEditor.emit(query)
  }

  copyQuery(query: string) {
    this.clipboard.copy(query);
    this.alerts.open('Copied to clipboard', {appearance: 'success'})
      .subscribe()
  }

  confirmRemoval() {
    this.dialogService
      .open<boolean>(TUI_CONFIRM, {
        label: 'Are you sure?',
        data: {
          content: "This will remove the previous chats for this tab",
          yes: 'Remove',
          no: 'Cancel',
        },
      })
      .subscribe(response => {
        if (response) this.deleteChatHistory.emit();
      });
  }

  private findQuery(index?: number): string {
    // if no index supplied, find the last chat message and use that
    const targetIndex = index || this.conversationMessages().length - 1;
    const query = this.conversationMessages()[targetIndex].chunks
      .map(chunk => chunk.kind === 'Query' ? chunk.taxi : null)
      .filter(val => val)[0]
    return query;
  }

  private scrollToBottom() {
    console.log("scrollToBottom")
    setTimeout(() => {
      this.results.nativeElement.scrollTop = this.results.nativeElement.scrollHeight;
    }, 30)
  }
}
