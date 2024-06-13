import {Inject, Injectable} from "@angular/core";
import {VyneServicesModule} from "./vyne-services.module";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Environment, ENVIRONMENT} from "./environment";


@Injectable({
  providedIn: VyneServicesModule
})
export class ContentService {

  constructor(private http: HttpClient,
              @Inject(ENVIRONMENT) private environment: Environment,
  ) {
  }

  getContent(): Observable<ContentCardData[]> {
    return this.http.get<ContentCardData[]>(`${this.environment.serverUrl}/api/cms/content`);
  }
}


export interface ContentCardData {
  title: string;
  text: string;
  action: {
    text: string;
    route: string[];
    external: boolean;
  }
  iconUrl: string;
}
