import requestPromise from "request-promise-native";

export interface VersionedSchema {
   name: string
   version: string,
   content: string
}

export class SchemaSpec {
   constructor(readonly name: string, readonly version: string, readonly defaultNamespace: string) {
   }
}

export type SchemaFormat = "taxi" | "swagger";

export class SchemaImportRequest {
   constructor(
      readonly spec: SchemaSpec,
      readonly format: SchemaFormat,
      readonly content: string) {
   }
}

export class SchemaService {
   constructor(private vyneUrl: string) {
   }

   submitSchema(schema: SchemaImportRequest): Promise<VersionedSchema> {
      let promise: requestPromise.RequestPromise = requestPromise.post(`${this.vyneUrl}/schemas`,
         {
            json: schema
         }
      );
      return promise.promise();
   }
}
