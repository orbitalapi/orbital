import { TableTaxiGenerationRequest } from '../db-connection-editor/db-importer.service';
import { PackageIdentifier } from '../package-viewer/packages.service';

export class ConvertSchemaEvent {
  constructor(public readonly schemaType: SchemaType, public readonly options: SchemaConverterOptions, public readonly packageIdentifier: PackageIdentifier) {
  }
}
export class SwaggerConverterOptions {
  public defaultNamespace: string;
  public serviceBasePath: string;
  public swagger: string;
  public url: string;
}

export class JsonSchemaConverterOptions {
  public defaultNamespace: string | null;
  public jsonSchema: string | null;
  public url: string | null;
  public resolveUrlsRelativeToUrl: string | null;
  public schemaVersion: JsonSchemaVersion = 'INFERRED';
}

export class ProtobufSchemaConverterOptions {
  public protobuf: string | null;
  public filename: string | null;
  public url: string | null;
}

export type JsonSchemaVersion = 'INFERRED' | 'DRAFT_6' | 'DRAFT_7';

export class TableSchemaConverterOptions {
  // public defaultNamespace: string | null;
  public connectionName: string;
  public tables: TableTaxiGenerationRequest[];
}

export type SchemaConverterOptions =
  SwaggerConverterOptions
  | JsonSchemaConverterOptions
  | TableSchemaConverterOptions
  | KafkaTopicConverterOptions
  | S3SchemaConverterOptions
  | ProtobufSchemaConverterOptions
  | MapConverterOptions;
export type SchemaType = 'jsonSchema' | 'swagger' | 'databaseTable' | 'kafkaTopic' | 'protobuf' | 'map' | 's3';

export type KafkaOffset = 'EARLIEST' | 'LATEST' | 'NONE';

export class KafkaTopicConverterOptions {
  public connectionName: string;
  public topicName: string;
  public offset: KafkaOffset;
  public messageType: string;
  public targetNamespace?: string;
  public serviceName?: string;
  public operationName?: string;
}

export class S3SchemaConverterOptions {
  public connectionName: string;
  public targetNamespace: string;
  public bucketName: string;
  public filenamePattern: string;
  public fileSchemaType: string;
  public serviceName?: string;
  public operationName?: string;
  public newModelName?: string;
  public fileToGenerateSchemaFrom?: string;
}

export interface MapConverterOptions {
  map: {[index: string]: any};
  createdTypeName: string;
  inheritedTypeName: string | null;
  fieldsToInclude: string[]
}
