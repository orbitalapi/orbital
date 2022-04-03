import {TableTaxiGenerationRequest} from '../db-connection-editor/db-importer.service';

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
  public url: string | null;
}

export type JsonSchemaVersion = 'INFERRED' | 'DRAFT_6' | 'DRAFT_7';

export class TableSchemaConverterOptions {
  // public defaultNamespace: string | null;
  public connectionName: string;
  public tables: TableTaxiGenerationRequest[];
}

export type SchemaConverterOptions = SwaggerConverterOptions | JsonSchemaConverterOptions | TableSchemaConverterOptions | KafkaTopicConverterOptions | ProtobufSchemaConverterOptions;
export type SchemaType = 'jsonSchema' | 'swagger' | 'databaseTable' | 'kafkaTopic' | 'protobuf';

export class ConvertSchemaEvent {
  constructor(public readonly schemaType: SchemaType, public readonly options: SchemaConverterOptions) {
  }
}

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
