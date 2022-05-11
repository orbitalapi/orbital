import {Component, Inject, OnInit} from '@angular/core';
import {TuiDialogContext} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';
import {ConnectorSummary, ConnectorType, JdbcConnectionConfiguration} from './db-importer.service';


export class ConnectionEditorContext {
  constructor(public readonly selectedDriverName: string | null = null, public readonly filterConnectionTypes: ConnectorType | null = null) {
  }
}
@Component({
  selector: 'app-connection-editor-dialog',
  template: `
    <app-connection-editor (connectionCreated)="onConnectionCreated($event)"
                           [filterConnectorTypes]="context.data?.filterConnectionTypes"
                           [selectedDriverId]="context.data?.selectedDriverName"></app-connection-editor>
  `,
  styleUrls: ['./db-connection-editor-dialog.component.scss']
})
export class DbConnectionEditorDialogComponent {

  constructor(@Inject(POLYMORPHEUS_CONTEXT) public readonly context: TuiDialogContext<ConnectorSummary, ConnectionEditorContext>) {
  }

  onConnectionCreated($event: ConnectorSummary) {
    this.context.completeWith($event);
  }
}
