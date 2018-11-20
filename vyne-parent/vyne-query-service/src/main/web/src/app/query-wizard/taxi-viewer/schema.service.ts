import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/internal/Observable";
import {environment} from "src/environments/environment";

@Injectable({
  providedIn: 'root'
})
export class SchemaService {

  constructor(private httpClient: HttpClient) {

  }

  // TODO : Should return Observable<Schema>, once SDK is available
  getSchemaForMembers(members: String[]): Observable<any> {
    const memberString = members.join("&members=");
    return this.httpClient.get(`${environment.queryServiceUrl}/schema?members=${memberString}`)
  }


}
