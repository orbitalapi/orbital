import {Metadata, QualifiedName, VersionedSource} from '../services/schema';
import {HttpClient} from '@angular/common/http';
import {environment} from 'src/environments/environment';
import {Observable} from 'rxjs';
import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {TaxiSubmissionResult} from '../services/types.service';
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
    // tslint:disable-next-line:max-line-length
    return this.http.get<TableMetadata>(`${environment.queryServiceUrl}/api/connections/jdbc/${connectionName}/tables/${schemaName}/${tableName}/metadata`);
  }

  generateTaxiForTable(connectionName: string, tables: TableTaxiGenerationRequest[]): Observable<TaxiSubmissionResult> {
    return this.http.post<TaxiSubmissionResult>
    (`${environment.queryServiceUrl}/api/connections/jdbc/${connectionName}/tables/taxi/generate`, {
      tables: tables,
    } as JdbcTaxiGenerationRequest);
  }

  submitModel(connectionName: string, schemaName: string, tableName: string, request: TableModelSubmissionRequest): Observable<any> {
    return this.http.post<TaxiSubmissionResult>
    (`${environment.queryServiceUrl}/api/connections/jdbc/${connectionName}/tables/${schemaName}/${tableName}/model`, request);
  }

  removeTypeMapping(connectionName: string, schemaName: string, tableName: string, typeName: QualifiedName): Observable<any> {
    return this.http.delete(
      // tslint:disable-next-line:max-line-length
      `${environment.queryServiceUrl}/api/connections/jdbc/${connectionName}/tables/${schemaName}/${tableName}/model/${typeName.parameterizedName}`);
  }
}

export interface JdbcTaxiGenerationRequest {
  tables: TableTaxiGenerationRequest[];
  namespace: string;
}

export interface TableTaxiGenerationRequest {
  table: JdbcTable;
  typeName?: NewOrExistingTypeName | null;
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

export type ConnectorType = 'JDBC' | 'KAFKA';

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

export interface ColumnMapping {
  name: string;
  typeSpec: TypeSpec;
  columnSpec: JdbcColumn;
}

export interface TableModelSubmissionRequest {
  model: TypeSpec;
  columnMappings: ColumnMapping[];
  serviceMappings: VersionedSource[];
}
