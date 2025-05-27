import {BehaviorSubject, Observable} from "rxjs";
import {emptySchema, Schema} from "src/app/services/schema";
import {SchemaProvider} from "../app/services/types.service";
import {Injectable} from "@angular/core";

@Injectable({
  providedIn: 'root'
})
export class PlaygroundSchemaService implements SchemaProvider {
  private schema$ = new BehaviorSubject<Schema>(emptySchema());
  getSchema(): Observable<Schema> {
    return this.schema$;
  }
  updateSchema(schema:Schema) {
    this.schema$.next(schema);
  }
}
