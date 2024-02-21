import {Inject, Injectable} from '@angular/core';
import {concatWith, Observable} from 'rxjs';
import {WebsocketService} from './websocket.service';
import {share, shareReplay} from 'rxjs/operators';
import {PackageIdentifier} from "../package-viewer/packages.service";
import {HttpClient} from "@angular/common/http";
import {Environment, ENVIRONMENT} from "./environment";

@Injectable({
  providedIn: 'root'
})
export class SchemaNotificationService {
  private readonly schemaUpdates$: Observable<SchemaUpdatedNotification>;

  constructor(private websocketService: WebsocketService,
              private http:HttpClient,
              @Inject(ENVIRONMENT) private environment: Environment,
              ) {

    // Initial state over http,
    // followed by stream of updates over websocket
    this.schemaUpdates$ = this.getSchemaSummary()
      .pipe(
        concatWith(websocketService.connect('/api/schema/updates')),
        shareReplay(1)
      );
  }

  private getSchemaSummary(): Observable<SchemaUpdatedNotification> {
    return this.http.get<SchemaUpdatedNotification>(`${this.environment.serverUrl}/api/schemas/summary`);
  }


  createSchemaNotificationsSubscription(): Observable<SchemaUpdatedNotification> {
    return this.schemaUpdates$;
  }
}

export interface SchemaUpdatedNotification {
  newId: number;
  generation: number;
  invalidSourceCount: number;
  sourceNamesWithErrors: SourceNameWithPackage[]
}

export interface SourceNameWithPackage {
  name: string;
  packageIdentifier: PackageIdentifier
}
