import {SchemaService} from "./schemas";

export class Vyne {
   readonly schemaService: SchemaService;

   constructor(url: string) {
      this.schemaService = new SchemaService(url);
   }
}
