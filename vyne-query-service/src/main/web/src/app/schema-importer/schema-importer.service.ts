import {Injectable} from '@angular/core';
import {VyneServicesModule} from '../services/vyne-services.module';
import {HttpClient} from '@angular/common/http';
import {ConvertSchemaEvent} from './schema-importer.models';
import {environment} from '../../environments/environment';
import {Observable} from 'rxjs/internal/Observable';
import {SchemaSubmissionResult} from '../services/types.service';

@Injectable({
  providedIn: VyneServicesModule
})
export class SchemaImporterService {
  constructor(private httpClient: HttpClient) {
  }

  convertSchema(event: ConvertSchemaEvent): Observable<SchemaSubmissionResult> {
    return this.httpClient.post<SchemaSubmissionResult>(`${environment.queryServiceUrl}/api/schemas/import?dryRun=true`, {
      format: event.schemaType,
      options: event.options
    } as SchemaConversionRequest)
  }
}

export interface SchemaConversionRequest {
  format: string;
  options: any;
}
