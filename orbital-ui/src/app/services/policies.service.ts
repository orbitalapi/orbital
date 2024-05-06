import {Inject, Injectable} from "@angular/core";
import {Environment, ENVIRONMENT} from "./environment";
import {HttpClient} from "@angular/common/http";
import {QualifiedName} from "./schema";
import {Observable} from "rxjs";
import {environment} from "../../environments/environment";


@Injectable({
  providedIn: 'root',
})
export class PoliciesService {
  constructor(
    @Inject(ENVIRONMENT) private environment: Environment,
    private http: HttpClient,
  ) {
  }

  getPolicySetupReadiness():Observable<PolicySetupReadiness> {
    return this.http.get<PolicySetupReadiness>(`${this.environment.serverUrl}/api/policies/readiness`)
  }

}

export interface PolicySetupReadiness {
  authenticationConfigured: boolean;
  claims: { [index: string]: any }
  authTokenTypes: QualifiedName[]
}
