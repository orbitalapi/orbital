import { Component, Output } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { EventEmitter } from 'stream';

@Component({
  selector: 'playground-toolbar',
  template: `
    <img src="assets/img/taxi-icon-white.svg" class="logo">
    <span class="title">Taxi playground</span>
    <div class="spacer"></div>
    <button class="header-icon-button">
      <a target="_blank" href="https://vyne.co"><mat-icon svgIcon="vyneDots"></mat-icon></a>
    </button>
    <button class="header-icon-button">
      <a target="_blank" href="https://github.com/orbitalapi/orbital">
        <mat-icon svgIcon="brandGitHub"></mat-icon>
      </a>
    </button>
    <button class="header-icon-button" (click)="slackInvite()">
      <mat-icon svgIcon="slack"></mat-icon>
    </button>
  `,
  styleUrls: ['./playground-toolbar.component.scss']
})
export class PlaygroundToolbarComponent {
  
  readonly slackInviteLink = "https://join.slack.com/t/vynehq/shared_invite/zt-1gxb15z4g-H2IeysSGo_rI1ptOty6mwA";
  
  constructor(public authService: AuthService) {
  }

  slackInvite() {
    window.open(this.slackInviteLink)
  }
}
