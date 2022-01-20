import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

@Component({
  selector: 'app-connection-manager',
  template: `
    <app-header-bar title="Connection manager">
      <button mat-flat-button color="primary" [mat-menu-trigger-for]="connectionTypeMenu">Add new connection...</button>
      <mat-menu #connectionTypeMenu="matMenu">
        <button mat-menu-item (click)="createNewDbConnection()">Database connection</button>
        <!--      <button mat-menu-item (click)="createNewKafkaConnection()">Kafka connection</button>-->
      </mat-menu>
    </app-header-bar>
    <div class="page-content">
      <div class="centered-page-block">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styleUrls: ['./connection-manager.component.scss']
})
export class ConnectionManagerComponent {

  constructor(private router: Router) {
  }

  createNewDbConnection() {
    this.router.navigate(['connection-manager', 'new', 'database']);
  }

  createNewKafkaConnection() {

  }
}
