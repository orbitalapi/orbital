import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ConnectionDriverConfigOptions, ConnectorSummary, DbConnectionService} from './db-importer.service';
import {Observable} from 'rxjs';
import {filter, mergeMap} from 'rxjs/operators';
import {ConnectionEditorMode} from './connection-editor.component';
import {PackagesService, SourcePackageDescription} from "../package-viewer/packages.service";

export type WizardStage = 'select-connection-type' | 'create-connection' | 'create-type';

@Component({
  selector: 'app-db-connection-wizard',
  template: `
    <app-connection-editor [drivers]="drivers"
                           [mode]="connectionEditorMode"
                           [connector]="connectionToEdit"
                           [packages$]="packages$"
    ></app-connection-editor>

  `,
  styleUrls: ['./db-connection-wizard.component.scss']
})
export class DbConnectionWizardComponent {
  drivers: ConnectionDriverConfigOptions[];

  connectionToEdit: ConnectorSummary;
  connectionEditorMode: ConnectionEditorMode = 'create';
  packages$: Observable<SourcePackageDescription[]>;

  constructor(dbConnectionService: DbConnectionService,
              activatedRoute: ActivatedRoute,
              packagesService: PackagesService) {
    dbConnectionService.getDrivers()
      .subscribe(drivers => this.drivers = drivers);

    activatedRoute.paramMap.pipe(
      filter(p => p.has('connectionName')),
      mergeMap(params => dbConnectionService.getConnection(params.get('connectionName')))
    ).subscribe(connection => {
      this.connectionToEdit = connection;
      this.connectionEditorMode = 'edit';
    });

    this.packages$ = packagesService.listPackages()
  }

}

