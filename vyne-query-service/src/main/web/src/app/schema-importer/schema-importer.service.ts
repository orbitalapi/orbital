import { Injectable } from '@angular/core';
import { VyneServicesModule } from '../services/vyne-services.module';
import { HttpClient } from '@angular/common/http';
import { ConvertSchemaEvent } from './schema-importer.models';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs/internal/Observable';
import { SchemaSubmissionResult } from '../services/types.service';
import { PartialSchema } from '../services/schema';
import { PackagesService, SourcePackageDescription } from '../package-viewer/packages.service';
import { switchMap } from 'rxjs/operators';

@Injectable({
  providedIn: VyneServicesModule,
})
export class SchemaImporterService {
  editablePackage$: Observable<SourcePackageDescription | null> = this.packagesService.getEditablePackage();

  constructor(private httpClient: HttpClient, private packagesService: PackagesService) {
  }

  convertSchema(event: ConvertSchemaEvent): Observable<SchemaSubmissionResult> {
    return this.httpClient.post<SchemaSubmissionResult>(`${environment.serverUrl}/api/schemas/import?validateOnly=true`, {
      format: event.schemaType,
      options: event.options,
    } as SchemaConversionRequest);
  }

  submitEditedSchema(schema: PartialSchema): Observable<any> {
    return this.editablePackage$.pipe(switchMap(editablePackage => {
      return this.httpClient.post(`${environment.serverUrl}/api/schemas/edit?packageIdentifier=${editablePackage.identifier.id}`, {
        types: schema.types,
        services: schema.services,
      });
    }));
  }
}

export interface SchemaConversionRequest {
  format: string;
  options: any;
}
