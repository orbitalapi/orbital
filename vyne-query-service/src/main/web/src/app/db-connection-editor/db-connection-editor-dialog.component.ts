import {Component, Inject, OnInit} from '@angular/core';
import {TuiDialogContext} from '@taiga-ui/core';
import {POLYMORPHEUS_CONTEXT} from '@tinkoff/ng-polymorpheus';
import {ConnectorSummary, JdbcConnectionConfiguration} from './db-importer.service';

@Component({
  selector: 'app-db-connection-editor-dialog',
  template: `
    <app-db-connection-editor (connectionCreated)="onConnectionCreated($event)"></app-db-connection-editor>
  `,
  styleUrls: ['./db-connection-editor-dialog.component.scss']
})
export class DbConnectionEditorDialogComponent {

  constructor(@Inject(POLYMORPHEUS_CONTEXT) private readonly context: TuiDialogContext<ConnectorSummary>) {
  }

  onConnectionCreated($event: ConnectorSummary) {
    this.context.completeWith($event);
  }
}
