import {Inject, Injectable} from "@angular/core";
import {AppInfoService} from "../services/app-info.service";
import {Environment, ENVIRONMENT} from "../services/environment";
import {UserInfoService} from "../services/user-info.service";
import {HttpClient} from "@angular/common/http";
import {combineLatestWith, concatMap, filter, Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class LicenseService {
  license: Observable<LicenseWithUsage>

  constructor(
    @Inject(ENVIRONMENT) private environment: Environment,
    private userInfoService: UserInfoService,
    private appInfoService: AppInfoService,
    private httpClient: HttpClient
  ) {
    userInfoService.getUserInfo()
      .pipe(
        combineLatestWith(appInfoService.getConfig()),
        filter(([vyneUser, appConfig]) => appConfig.featureToggles.enableLicenseEnforcement),
        concatMap(() => {
          // fetch the license when the user changes.
          // Do this eagerly (ie., as soon as the user signs in), as it
          // triggers download of a license if the user doesn't have one
          return this.getLicenseDetails()
        })
      )
      .subscribe()
  }

  /**
   * Requests the current license status.
   * Importantly, for self-hosted users, this triggers downloading
   * a license if the user doesn't already have one.
   *
   * Must be authenticated
   */
  getLicenseDetails(refresh: boolean = false): Observable<LicenseWithUsage> {
    return this.license = this.httpClient.get<LicenseWithUsage>(`${this.environment.serverUrl}/api/license/status?refresh=${refresh}`)
  }
}

export interface LicenseWithUsage {
  licenseSummary: LicenseSummary,
  usage: QuotaUsageState[]
}

export interface LicenseSummary {
  licensee: string;
  expiresOn: Date;
  plan: PlanType,
}

export enum PlanType {
  Free = 'Free',
  Project = 'Project',
  Platform = 'Platform',
  Enterprise = 'Enterprise'
}

export interface QuotaUsageState {
  quota: UsageQuote
  usage: number
  health: QuotaHealth
}

export type QuotaHealth = 'HEALTHY' | 'WARNING' | 'EXCEEDED';

export interface UsageQuote {
  capability: MeteredCapability;
  limit: number;
}

export type MeteredCapability = 'User' | 'Endpoint' | 'Invocation';
