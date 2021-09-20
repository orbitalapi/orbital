import {QualifiedName} from '../services/schema';
import {HttpClient} from '@angular/common/http';
import {environment} from 'src/environments/environment';
import {Observable} from 'rxjs';
import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {DbConnectionEditorModule} from './db-connection-editor.module';


export interface TableColumn {
  columnName: string;
  dataType: string;
  columnSize?: number;
  decimalDigits?: number;
  nullable: boolean;

  // clientSideOnly:
  taxiType: QualifiedName | null;
}

export interface TableMetadata {
  columns: TableColumn[];
  name: string;
}

export interface JdbcDriverConfigOptions {
  driverName: string;
  displayName: string;
  parameters: JdbcConnectionParam[];
}

export interface JdbcConnectionParam {
  displayName: string;
  dataType: SimpleDataType;
  defaultValue: any | null;
  sensitive: boolean;
  required: boolean;
  templateParamName: string;
  allowedValues: any[];
}

export type SimpleDataType = 'STRING' | 'NUMBER' | 'BOOLEAN';

export interface JdbcConnectionConfiguration {
  name: string;
  jdbcDriver: string;
  connectionParameters: { [key: string]: any };
}

@Injectable({
  providedIn: VyneServicesModule
})
export class DbConnectionService {
  constructor(private http: HttpClient) {
  }

  getDrivers(): Observable<JdbcDriverConfigOptions[]> {
    return this.http.get<JdbcDriverConfigOptions[]>(`${environment.queryServiceUrl}/api/connections/jdbc/drivers`);
  }

  getConnections(): Observable<ConnectorSummary[]> {
    return this.http.get<ConnectorSummary[]>(`${environment.queryServiceUrl}/api/connections/jdbc`);
  }

  testConnection(connectionConfig: JdbcConnectionConfiguration): Observable<any> {
    return this.http.post(`${environment.queryServiceUrl}/api/connections/jdbc?test=true`, connectionConfig);
  }

  createConnection(connectionConfig: JdbcConnectionConfiguration): Observable<any> {
    return this.http.post(`${environment.queryServiceUrl}/api/connections/jdbc`, connectionConfig);
  }

  getMappedTablesForConnection(connectionName: string): Observable<MappedTable[]> {
    return this.http.get<MappedTable[]>(`${environment.queryServiceUrl}/api/connections/jdbc/${connectionName}/tables`);
  }

  getColumns(connectionName: string, schemaName: string, tableName: string): Observable<TableMetadata> {
    return this.http.get<TableMetadata>(`${environment.queryServiceUrl}/api/connections/jdbc/${connectionName}/tables/${schemaName}/${tableName}/metadata`);
  }
}

export interface ConnectorSummary {
  connectionName: string;
  driverName: string;
  address: string;
  type: ConnectorType;
}

export type ConnectorType = 'JDBC' | 'KAFKA';

export interface MappedTable {
  table: JdbcTable;
  mappedTo: QualifiedName | null;
}

export interface JdbcTable {
  schemaName: string;
  tableName: string;
}

