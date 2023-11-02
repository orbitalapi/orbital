import {Inject, Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {CompilationMessage, QualifiedName, Type} from "../services/schema";
import {Observable} from "rxjs/internal/Observable";
import {ENVIRONMENT, Environment} from 'src/app/services/environment';

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

export interface TaxiParseResult {
  parseResult: ModelParseResult | null;
  newTypes: Type[]
  compilationErrors: CompilationMessage[];
  hasParseErrors: boolean;
  hasCompilationErrors: boolean;
  hasErrors: boolean;
}

export interface SourceLocation {
  line: number;
  char: number;
}
export interface TypePosition {
  start: SourceLocation
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
