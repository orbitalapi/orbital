import {Component, Inject, Injector} from '@angular/core';
import {Message, Severity, severityToTuiNotification} from "../services/schema";
import {TuiAlertOptions, TuiAlertService, TuiButtonModule, TuiNotificationT} from "@taiga-ui/core";
import {POLYMORPHEUS_CONTEXT, PolymorpheusComponent} from "@tinkoff/ng-polymorpheus";
import {TuiDialog} from "@taiga-ui/cdk";
import {merge, Observable} from "rxjs";
import {ResultWithMessage} from "../services/types.service";

@Component({
  selector: 'app-alert-with-dismiss',
  standalone: true,
  imports: [
    TuiButtonModule
  ],
  template: `
    <div class="message-text" [innerHTML]="messageWithLineBreaks"></div>
    <button tuiButton size="s" appearance="outline" (click)="close()">Close</button>
  `,
  styleUrl: './alert-with-dismiss.component.scss'
})
export class AlertWithDismissComponent {
  readonly message: Message;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    private readonly context: TuiDialog<TuiAlertOptions<Message>, void>
  ) {
    this.message = context.data;
  }

  get messageWithLineBreaks(): string {
    return this.message.message.replaceAll("\n", "<br />")
  }

  close() {
    this.context.completeWith();
  }
}

export function showAlertForMessages(result: ResultWithMessage, severity: Severity, alertService: TuiAlertService, injector: Injector): Observable<void> {
  return merge(...result.messages
    .filter(m => m.severity === severity)
    .map(m => showAlertForMessage(m, alertService, injector))
  )
}

export function showAlertForMessage(message: Message, alertService: TuiAlertService, injector: Injector): Observable<void> {
  return alertService.open(
    new PolymorpheusComponent(AlertWithDismissComponent, injector),
    {
      status: severityToTuiNotification(message.severity),
      data: message,
      autoClose: false,
      hasCloseButton: true
    }
  )
}
