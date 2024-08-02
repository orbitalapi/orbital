import {Inject, Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {CompilationMessage, QualifiedName, Type} from "../services/schema";
import {Observable} from "rxjs/internal/Observable";
import {ENVIRONMENT, Environment} from 'src/app/services/environment';
import {SourceWithTypeHints} from "../json-viewer/json-results-view.component";

@Injectable()
export class TaxiParserService {
  constructor(private httpClient: HttpClient, @Inject(ENVIRONMENT) private environment: Environment) {
  }

  parseTaxiModel(request: TaxiParseRequest): Observable<TaxiParseResult> {
    return this.httpClient.post<TaxiParseResult>(`${this.environment.serverUrl}/api/taxi/parseModel`, request)
  }
}

export interface TaxiParseRequest {
  taxi: string | null;
  model: ModelParseRequest | null;
}

export interface ModelParseRequest {
  model: string;
  targetType: string;
  includeTypeInformation: boolean;
}

export function toSourceWithTypeHints(parseResult: ModelParseResult):SourceWithTypeHints {
  return {
    source: parseResult.json,
    typeHints: parseResult.typeHints
  }
}
export interface TaxiParseResult {
  parseResult: ModelParseResult | null;
  newTypes: Type[]
  compilationErrors: CompilationMessage[];
  hasParseErrors: boolean;
  hasCompilationErrors: boolean;
  hasErrors: boolean;
}

// called SourceLocation on the server.
export interface Position {
  line: number;
  char: number;
}
export interface TypePosition {
  start: Position
  /**
   * The number of characters in total from the start
   * that this position begins.
   */
  startOffset: number;
  path: string;
  type: QualifiedName;
}
export interface ModelParseResult {
  raw: any | any[];
  typeHints: TypePosition[];
  parseErrors: string[];
  json: string;
}
