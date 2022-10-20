import {Metadata, QualifiedName, VersionedSource} from '../services/schema';
import {HttpClient} from '@angular/common/http';
import {environment} from 'src/environments/environment';
import {Observable} from 'rxjs';
import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {SchemaSubmissionResult} from '../services/types.service';
import {NewTypeSpec} from '../type-editor/type-editor.component';


export interface JdbcColumn {
  columnName: string;
  dataType: string;
  columnSize?: number;
  decimalDigits?: number;
  nullable: boolean;
}

export interface TableModelMapping {
  typeSpec: NewTypeSpec;
  tableMetadata: TableMetadata;
}

export interface TableMetadata {
  connectionName: string;
  schemaName: string;
  tableName: string;
  mappedType: QualifiedName | null;
  columns: ColumnMapping[];
}


export interface ConnectionDriverConfigOptions {
  driverName: string;
  displayName: string;
  connectorType: ConnectorType;
  parameters: ConnectionParam[];
}

export interface ConnectionParam {
  displayName: string;
  dataType: SimpleDataType;
  defaultValue: any | null;
  sensitive: boolean;
  required: boolean;
  visible: boolean;
  templateParamName: string;
  allowedValues: any[];
}

export type ConnectorType = 'JDBC' | 'MESSAGE_BROKER' | 'AWS' | 'AZURE_STORAGE';
export type SimpleDataType = 'STRING' | 'NUMBER' | 'BOOLEAN';

export interface JdbcConnectionConfiguration {
  connectionName: string;
  jdbcDriver: string;
  connectionType: ConnectorType;
  connectionParameters: { [key: string]: any };
}

export interface MessageBrokerConfiguration {
  connectionName: string;
  driverName: string;
  connectionType: ConnectorType;
  connectionParameters: { [key: string]: any };
}

export interface AwsConnectionConfiguration {
  connectionName: string;
  driverName: string;
  connectionType: ConnectorType;
  connectionParameters: { [key: string]: any };
}

@Injectable({
  providedIn: VyneServicesModule
})
export class DbConnectionService {
  constructor(private http: HttpClient) {
  }

  getDrivers(): Observable<ConnectionDriverConfigOptions[]> {
    return this.http.get<ConnectionDriverConfigOptions[]>(`${environment.serverUrl}/api/connections/drivers`);
  }

  getConnection(name: string): Observable<ConnectorSummary> {
    return this.http.get<ConnectorSummary>(`${environment.serverUrl}/api/connections/jdbc/${name}`);
  }

  getConnections(): Observable<ConnectorSummary[]> {
    return this.http.get<ConnectorSummary[]>(`${environment.serverUrl}/api/connections`);
  }

  testConnection(connectionConfig: JdbcConnectionConfiguration | MessageBrokerConfiguration | AwsConnectionConfiguration): Observable<any> {
    const url = DbConnectionService.getConnectionUrl(connectionConfig);
    return this.http.post(`${environment.serverUrl}${url}?test=true`, connectionConfig);
  }

  createConnection(connectionConfig: JdbcConnectionConfiguration | MessageBrokerConfiguration | AwsConnectionConfiguration): Observable<ConnectorSummary> {
    const url = DbConnectionService.getConnectionUrl(connectionConfig);
    return this.http.post<ConnectorSummary>(`${environment.serverUrl}${url}`, connectionConfig);
  }

  private static getConnectionUrl(connectionConfig: JdbcConnectionConfiguration | MessageBrokerConfiguration): string {
    switch (connectionConfig.connectionType) {
      case 'JDBC':
        return '/api/connections/jdbc';
      case 'MESSAGE_BROKER':
        return '/api/connections/message-broker';
      case 'AWS':
        return '/api/connections/aws';
      case 'AZURE_STORAGE':
        return '/api/connections/azure_storage';
    }
  }

  getMappedTablesForConnection(connectionName: string): Observable<MappedTable[]> {
    return this.http.get<MappedTable[]>(`${environment.serverUrl}/api/connections/jdbc/${connectionName}/tables`);
  }

  getColumns(connectionName: string, schemaName: string, tableName: string): Observable<TableMetadata> {
    // eslint-disable-next-line max-len
    return this.http.get<TableMetadata>(`${environment.serverUrl}/api/connections/jdbc/${connectionName}/tables/${schemaName}/${tableName}/metadata`);
  }

  generateTaxiForTable(connectionName: string, tables: TableTaxiGenerationRequest[]): Observable<SchemaSubmissionResult> {
    return this.http.post<SchemaSubmissionResult>
    (`${environment.serverUrl}/api/connections/jdbc/${connectionName}/tables/taxi/generate`, {
      tables: tables,
    } as JdbcTaxiGenerationRequest);
  }

  submitModel(connectionName: string, schemaName: string, tableName: string, request: TableModelSubmissionRequest): Observable<any> {
    return this.http.post<SchemaSubmissionResult>
    (`${environment.serverUrl}/api/connections/jdbc/${connectionName}/tables/${schemaName}/${tableName}/model`, request);
  }

  removeTypeMapping(connectionName: string, schemaName: string, tableName: string, typeName: QualifiedName): Observable<any> {
    return this.http.delete(
      // eslint-disable-next-line max-len
      `${environment.serverUrl}/api/connections/jdbc/${connectionName}/tables/${schemaName}/${tableName}/model/${typeName.parameterizedName}`);
  }
}

export interface JdbcTaxiGenerationRequest {
  tables: TableTaxiGenerationRequest[];
  namespace: string;
}

export interface TableTaxiGenerationRequest {
  table: JdbcTable;
  typeName?: NewOrExistingTypeName | null;
  defaultNamespace?: string | null;
}

export interface NewOrExistingTypeName {
  typeName: string;
  exists: boolean;
}

export interface ConnectorSummary {
  connectionName: string;
  driverName: string;
  address: string;
  type: ConnectorType;
}

export interface MappedTable {
  table: JdbcTable;
  mappedTo: QualifiedName | null;
}

export interface JdbcTable {
  schemaName: string;
  tableName: string;
}

export interface TypeSpec {
  typeName: QualifiedName | null;
  taxi: VersionedSource | null;
  metadata: Metadata[];
}

export interface ColumnMapping extends TypeSpecContainer {
  name: string;
  typeSpec: TypeSpec;
  columnSpec: JdbcColumn;
}

export interface TypeSpecContainer {
  typeSpec: TypeSpec;
}

export interface TableModelSubmissionRequest {
  model: TypeSpec;
  columnMappings: ColumnMapping[];
  serviceMappings: VersionedSource[];
}
