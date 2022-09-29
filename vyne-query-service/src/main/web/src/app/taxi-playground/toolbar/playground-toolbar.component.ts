import { Component, Inject, Injector } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { TuiDialogService } from '@taiga-ui/core';
import { SubscribeDialogComponent } from '../subscribe-dialog/subscribe-dialog.component';
import {PolymorpheusComponent} from '@tinkoff/ng-polymorpheus';

@Component({
  selector: 'playground-toolbar',
  templateUrl: './playground-toolbar.component.html',
  styleUrls: ['./playground-toolbar.component.scss']
})
export class PlaygroundToolbarComponent {

  readonly slackInviteLink = "https://join.slack.com/t/vynehq/shared_invite/zt-1gxb15z4g-H2IeysSGo_rI1ptOty6mwA";

  constructor(
    public authService: AuthService,
    @Inject(Injector) private readonly injector: Injector,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService) {
  }

  slackInvite() {
    window.open(this.slackInviteLink)
  }

  showSubscribeDialog = false;

  subscribeForUpdates() {
    console.log("argh")
    this.dialog.subscribe({
      next: data => {
        console.info(`Dialog emitted data = ${data}`);
      },
      complete: () => {
          console.info(`Dialog closed`);
      },
    });
  }

  private readonly dialog = this.dialogService.open(
    new PolymorpheusComponent(SubscribeDialogComponent, this.injector));
}
