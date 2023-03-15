import { TableTaxiGenerationRequest } from '../db-connection-editor/db-importer.service';
import { PackageIdentifier } from 'src/app/package-viewer/packages.service';

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

export type SchemaConverterOptions =
  SwaggerConverterOptions
  | JsonSchemaConverterOptions
  | TableSchemaConverterOptions
  | KafkaTopicConverterOptions
  | ProtobufSchemaConverterOptions;
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


export class GitRepositoryConfig {
  public name: string;
  public uri: string;
  public branch: string;

  public pullRequestConfig: GitPullRequestConfig | null = new GitPullRequestConfig();
  public isEditable: boolean = false;

  public path: string = '/'

  public loader: PackageLoaderSpec = new TaxiPackageLoaderSpec();
}


export class FileSystemPackageSpec {
  public path: string;
  public loader: PackageLoaderSpec = new TaxiPackageLoaderSpec();
  public isEditable: boolean = true;

  // Part of CreateFileRepositoryRequest, but shoe-horning here, as these
  // classes are otherwise identical
  newProjectIdentifier: PackageIdentifier | null = null
}

export type LoadablePackageType = 'OpenApi' | 'Taxi' | 'Protobuf' | 'JsonSchema';

interface PackageLoaderSpec {
  packageType: LoadablePackageType;
}

export class GitPullRequestConfig {
  public branchPrefix: string = 'schema-updates/';
  public hostingProvider: GitHostingProvider = 'Github';
}

export type GitHostingProvider = 'Github' | 'Gitlab';

export class TaxiPackageLoaderSpec implements PackageLoaderSpec {
  readonly packageType = 'Taxi';
}

export class OpenApiPackageLoaderSpec implements PackageLoaderSpec {
  readonly packageType = 'OpenApi'
  identifier: PackageIdentifier = {
    name: null,
    organisation: null,
    version: null,
    id: null,
    unversionedId: null
  }
  defaultNamespace: string;
  serviceBasePath: string;
}
