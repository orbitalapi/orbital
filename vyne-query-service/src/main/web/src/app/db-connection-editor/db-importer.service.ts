import {QualifiedName} from '../services/schema';
import {HttpClient} from '@angular/common/http';
import {environment} from 'src/environments/environment';
import {Observable} from 'rxjs';
import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {DbConnectionEditorModule} from './db-connection-editor.module';

export interface TableColumn {
  name: string;
  dbType: string;
  taxiType: QualifiedName;
  nullable: boolean;
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
  driver: string;
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

  getConnections(): Observable<ConfiguredConnectionSummary[]> {
    return this.http.get<ConfiguredConnectionSummary[]>(`${environment.queryServiceUrl}/api/connections/jdbc`);
  }

  testConnection(connectionConfig: JdbcConnectionConfiguration): Observable<any> {
    return this.http.post(`${environment.queryServiceUrl}/api/connections/jdbc?test=true`, connectionConfig);
  }

  createConnection(connectionConfig: JdbcConnectionConfiguration): Observable<any> {
    return this.http.post(`${environment.queryServiceUrl}/api/connections/jdbc`, connectionConfig);
  }

  getMappedTablesForConnection(connectionName: string): Observable<MappedTable[]> {
    return this.http.get<MappedTable[]>(`${environment.queryServiceUrl}/api/connections/jdbc/${connectionName}/tables`)
  }
}

export interface ConfiguredConnectionSummary {
  connectionName: string;
}

export interface MappedTable {
  table: JdbcTable;
  mappedTo: QualifiedName | null;
}

export interface JdbcTable {
  schemaName: string;
  tableName: string;
}
