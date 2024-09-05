import {Metadata, QualifiedName, SchemaMember, SchemaMemberReference, VersionedSource} from '../services/schema';
import {HttpClient} from '@angular/common/http';
import {environment} from 'src/environments/environment';
import {Observable} from 'rxjs';
import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {ResultWithMessage, SchemaSubmissionResult} from '../services/types.service';
import {NewTypeSpec} from 'src/app/type-editor/new-type-spec';
import {PackageIdentifier} from "../package-viewer/packages.service";


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

export type ConnectorType = 'JDBC' | 'MESSAGE_BROKER' | 'AWS' | 'AZURE_STORAGE' | 'NO_SQL';
export type SimpleDataType = 'STRING' | 'NUMBER' | 'BOOLEAN';

export interface JdbcConnectionConfiguration {
  connectionName: string;
  jdbcDriver: string;
  type: ConnectorType;
  connectionParameters: { [key: string]: any };
}

export interface MessageBrokerConfiguration {
  connectionName: string;
  driverName: string;
  type: ConnectorType;
  connectionParameters: { [key: string]: any };
}

export interface AwsConnectionConfiguration {
  connectionName: string;
  driverName: string;
  type: ConnectorType;
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
  getConnectionByName(packageUri: string, name: string): Observable<ConnectorConfigDetail> {
    return this.http.get<ConnectorConfigDetail>(`${environment.serverUrl}/api/connections/${packageUri}/${name}`);
  }

  getConnections(withUsages?: boolean): Observable<ConnectionsListResponse> {
    return this.http.get<ConnectionsListResponse>(`${environment.serverUrl}/api/connections?withUsages=${withUsages ? 'true' : 'false'}`);
  }

  testConnection(packageIdentifier: PackageIdentifier, connectionConfig: JdbcConnectionConfiguration | MessageBrokerConfiguration | AwsConnectionConfiguration): Observable<ConnectionStatus> {
    const url = DbConnectionService.getConnectionUrl(packageIdentifier, connectionConfig);
    return this.http.post<ConnectionStatus>(`${environment.serverUrl}${url}?test=true`, connectionConfig);
  }

  createConnection(packageIdentifier: PackageIdentifier, connectionConfig: JdbcConnectionConfiguration | MessageBrokerConfiguration | AwsConnectionConfiguration): Observable<ConnectorSummary> {
    const url = DbConnectionService.getConnectionUrl(packageIdentifier, connectionConfig);
    return this.http.post<ConnectorSummary>(`${environment.serverUrl}${url}`, connectionConfig);
  }

  private static getConnectionUrl(packageIdentifier: PackageIdentifier, connectionConfig: JdbcConnectionConfiguration | MessageBrokerConfiguration): string {
    switch (connectionConfig.type) {
      case 'JDBC':
        return `/api/packages/${packageIdentifier.uriSafeId}/connections/jdbc`;
      case 'MESSAGE_BROKER':
        return `/api/packages/${packageIdentifier.uriSafeId}/connections/message-broker`;
      case 'AWS':
        return `/api/packages/${packageIdentifier.uriSafeId}/connections/aws`;
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

export interface ConnectionsListResponse {
  definitionsWithErrors: PackageWithError[]
  connections: ConnectorSummary[]
}

export interface PackageWithError {
  identifier: PackageIdentifier;
  error: string;
  configFileName?: string;
}
export interface ConnectorSummary extends ResultWithMessage {
  connectionName: string;
  connectionType: ConnectorType;
  driverName: string;
  properties: { [index: string]: string }
  packageIdentifier: PackageIdentifier;
  connectionStatus: ConnectionStatus;
  usages?: SchemaMemberReference[]
}



export interface ConnectionStatus {
  status: 'OK' | 'ERROR' | 'UNKNOWN' | 'CONNECTING';
  timestamp: Date;
  message: string;
}

export interface ConnectorConfigDetail {
  config: ConnectorSummary;
  usages: SchemaMemberReference[];
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
