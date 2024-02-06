import {Component, Inject, OnInit} from '@angular/core';
import {TuiDialogContext} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';
import {ConnectorSummary, ConnectorType, JdbcConnectionConfiguration} from './db-importer.service';
import {PackagesService, SourcePackageDescription} from "../package-viewer/packages.service";
import {Observable} from "rxjs";


export class ConnectionEditorContext {
  constructor(public readonly selectedDriverName: string | null = null, public readonly filterConnectionTypes: ConnectorType | null = null) {
  }
}
@Component({
  selector: 'app-connection-editor-dialog',
  template: `
    <app-connection-editor (connectionCreated)="onConnectionCreated($event)"
                           [filterConnectorTypes]="context.data?.filterConnectionTypes"
                           [packages$]="packages$"
                           [selectedDriverId]="context.data?.selectedDriverName"></app-connection-editor>
  `,
  styleUrls: ['./db-connection-editor-dialog.component.scss']
})
export class DbConnectionEditorDialogComponent {
  packages$: Observable<SourcePackageDescription[]>;

  constructor(@Inject(POLYMORPHEUS_CONTEXT) public readonly context: TuiDialogContext<ConnectorSummary, ConnectionEditorContext>,
              private packagesService: PackagesService
              ) {
    this.packages$ = packagesService.listPackages()
  }

  onConnectionCreated($event: ConnectorSummary) {
    this.context.completeWith($event);
  }
}
