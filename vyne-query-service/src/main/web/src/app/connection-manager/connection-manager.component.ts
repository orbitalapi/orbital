import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

@Component({
  selector: 'app-connection-manager',
  templateUrl: './connection-manager.component.html',
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
