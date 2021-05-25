import {Component, OnInit} from '@angular/core';

@Component({
  selector: 'app-db-connection-editor',
  templateUrl: './db-connection-editor.component.html',
  styleUrls: ['./db-connection-editor.component.scss']
})
export class DbConnectionEditorComponent {
  connectionTypes = dbConnectionTypes;
  connection: DbConnection = {};

}

export interface DbConnection {
  connectionName?: string;
  connectionType?: string;
  host?: string;
  port?: string;
  database?: string;
  username?: string;
  password?: string;
}

export interface DbConnectionType {
  label: string;
  connectionType: string;
}

const dbConnectionTypes: DbConnectionType[] = [
  {label: 'Postgres', connectionType: 'postgres'},
  {label: 'MySql', connectionType: 'mysql'},
];
