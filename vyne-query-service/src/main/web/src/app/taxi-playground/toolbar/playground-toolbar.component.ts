import { Component } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'playground-toolbar',
  template: `
    <img src="assets/img/taxi-icon.svg" class="logo">
    <span class="title">Taxi playground</span>
    <div class="spacer"></div>
    <button class="secondary" (click)="authService.loginWithRedirect()">Save your design in Orbital</button>

  `,
  styleUrls: ['./playground-toolbar.component.scss']
})
export class PlaygroundToolbarComponent {

  constructor(public authService: AuthService) {
  }


}
