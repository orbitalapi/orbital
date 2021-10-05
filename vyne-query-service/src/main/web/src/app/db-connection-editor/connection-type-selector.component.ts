import {Component, EventEmitter, OnInit, Output} from '@angular/core';

@Component({
  selector: 'app-connection-type-selector',
  template: `
    <h2>Select how you'd like Vyne to connect your database</h2>
    <div class="row">
      <div class="selection-box">
        <div class="title-bar">
          <img src="assets/img/gateway.svg">
          <h3>Sidecar service</h3>
        </div>
        <p>Deploy a service that sits in front of your database, and acts as a gateway between Vyne and your
          database.</p>
        <p>Allows for more secure connections to a database (such as Kerberos or SSH connections)</p>
      </div>
      <div class="selection-box" (click)="createDirectConnection.emit()">
        <div class="title-bar">
          <img src="assets/img/database.svg">
          <h3>Direct</h3>
        </div>
        <p>Vyne connects directly to your database.  User credentials (if required) are provided and stored in Vyne.</p>
        <p>Easy to get going.  Requires a username and password for access to the database.</p>
      </div>
    </div>
  `,
  styleUrls: ['./connection-type-selector.component.scss']
})
export class ConnectionTypeSelectorComponent {

  @Output()
  createDirectConnection = new EventEmitter<void>();
}
