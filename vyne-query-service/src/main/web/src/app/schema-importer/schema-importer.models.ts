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

export type JsonSchemaVersion = 'INFERRED' | 'DRAFT_6' | 'DRAFT_7';

export class TableSchemaConverterOptions {
  public defaultNamespace: string | null;
  public connectionName: string;
  public tableName: string;
}
