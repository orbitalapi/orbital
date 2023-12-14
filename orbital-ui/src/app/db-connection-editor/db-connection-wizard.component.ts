import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ConnectorSummary, DbConnectionService, ConnectionDriverConfigOptions} from './db-importer.service';
import {Observable} from 'rxjs/index';
import {filter, mergeMap} from 'rxjs/operators';
import {isNullOrUndefined} from 'util';
import {ConnectionEditorMode} from './connection-editor.component';

export type WizardStage = 'select-connection-type' | 'create-connection' | 'create-type';

@Component({
  selector: 'app-db-connection-wizard',
  template: `
    <app-connection-editor [drivers]="drivers" [mode]="connectionEditorMode" [connector]="connectionToEdit"></app-connection-editor>

  `,
  styleUrls: ['./db-connection-wizard.component.scss']
})
export class DbConnectionWizardComponent {
  drivers: ConnectionDriverConfigOptions[];

  connectionToEdit: ConnectorSummary;
  connectionEditorMode: ConnectionEditorMode = 'create';

  constructor(private dbConnectionService: DbConnectionService, private activatedRoute: ActivatedRoute) {
    dbConnectionService.getDrivers()
      .subscribe(drivers => this.drivers = drivers);

    activatedRoute.paramMap.pipe(
      filter(p => p.has('connectionName')),
      mergeMap(params => dbConnectionService.getConnection(params.get('connectionName')))
    ).subscribe(connection => {
      this.connectionToEdit = connection;
      this.connectionEditorMode = 'edit';
    });
  }

}

