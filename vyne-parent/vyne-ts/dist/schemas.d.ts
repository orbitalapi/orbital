export interface VersionedSchema {
    name: string;
    version: string;
    content: string;
}
export declare class SchemaSpec {
    readonly name: string;
    readonly version: string;
    readonly defaultNamespace: string;
    constructor(name: string, version: string, defaultNamespace: string);
}
export declare type SchemaFormat = "taxi" | "swagger";
export declare class SchemaImportRequest {
    readonly spec: SchemaSpec;
    readonly format: SchemaFormat;
    readonly content: string;
    constructor(spec: SchemaSpec, format: SchemaFormat, content: string);
}
export declare class SchemaService {
    private vyneUrl;
    constructor(vyneUrl: string);
    submitSchema(schema: SchemaImportRequest): Promise<VersionedSchema>;
}
//# sourceMappingURL=schemas.d.ts.map