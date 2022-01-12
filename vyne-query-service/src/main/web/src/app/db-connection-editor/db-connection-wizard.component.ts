import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {DbConnectionService, JdbcDriverConfigOptions} from './db-importer.service';
import {Observable} from 'rxjs/index';

export type WizardStage = 'select-connection-type' | 'create-connection' | 'create-type';

@Component({
  selector: 'app-db-connection-wizard',
  template: `
    <div [ngSwitch]="wizardStage" class="container">
<!--      <app-connection-type-selector *ngSwitchCase="'select-connection-type'"-->
<!--                                    (createDirectConnection)="wizardStage = 'create-connection'"></app-connection-type-selector>-->
      <app-db-connection-editor *ngSwitchCase="'create-connection'" [drivers]="drivers"></app-db-connection-editor>
    </div>

  `,
  styleUrls: ['./db-connection-wizard.component.scss']
})
export class DbConnectionWizardComponent {
  drivers: JdbcDriverConfigOptions[];
  constructor(router: Router, private dbConnectionService: DbConnectionService) {
    dbConnectionService.getDrivers()
      .subscribe(drivers => this.drivers = drivers);
  }

  wizardStage: WizardStage = 'create-connection';


}

