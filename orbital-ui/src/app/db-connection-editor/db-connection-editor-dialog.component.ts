import { Component, Inject } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { TuiDialogContext } from '@taiga-ui/core';
import { POLYMORPHEUS_CONTEXT } from '@tinkoff/ng-polymorpheus';
import { ConnectorSummary, ConnectorType } from './db-importer.service';
import { PackageIdentifier, PackagesService, SourcePackageDescription } from '../package-viewer/packages.service';
import { ConnectionEditorComponent, ConnectionEditorMode } from './connection-editor.component';


export class ConnectionEditorContext {
  constructor(
    public readonly selectedDriverName: string | null = null,
    public readonly filterConnectionTypes: ConnectorType | null = null,
    public readonly mode: ConnectionEditorMode | null = null,
    public readonly selectedPackageId: PackageIdentifier | null = null
  ) {
  }
}

@Component({
  selector: 'app-connection-editor-dialog',
  template: `
    <app-connection-editor (connectionCreated)="onConnectionCreated($event)"
                           [filterConnectorTypes]="context.data?.filterConnectionTypes"
                           [packages$]="packages$"
                           [mode]="context.data.mode"
                           [selectedPackage]="selectedPackage"
                           [selectedDriverId]="context.data?.selectedDriverName"></app-connection-editor>
  `,
  styleUrls: ['./db-connection-editor-dialog.component.scss'],
  imports: [
    ConnectionEditorComponent
  ],
  standalone: true
})
export class DbConnectionEditorDialogComponent {
  packages$: Observable<SourcePackageDescription[]>;
  selectedPackage: SourcePackageDescription;

  constructor(@Inject(POLYMORPHEUS_CONTEXT) public readonly context: TuiDialogContext<ConnectorSummary, ConnectionEditorContext>,
              private packagesService: PackagesService,
  ) {
    this.packages$ = packagesService.listPackages()
      .pipe(
        tap(packages => this.selectedPackage = packages.find(pkg => pkg.identifier.id === this.context.data.selectedPackageId.id)))
  }

  onConnectionCreated($event: ConnectorSummary) {
    this.context.completeWith($event);
  }
}
