import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

@Component({
  selector: 'app-connection-manager',
  template: `
    <app-header-bar title="Connection manager">
    </app-header-bar>
    <div class="page-content centered-page-block-container">
      <div class="centered-page-block">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styleUrls: ['./connection-manager.component.scss']
})
export class ConnectionManagerComponent {

}
