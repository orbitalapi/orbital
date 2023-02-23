import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-connection-manager',
  template: `
    <app-header-bar>
    </app-header-bar>
    <router-outlet></router-outlet>
  `,
  styleUrls: ['./connection-manager.component.scss']
})
export class ConnectionManagerComponent {

}
