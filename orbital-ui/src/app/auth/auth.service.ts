import {Inject, Injectable} from '@angular/core';
import {AuthConfig, OAuthService} from 'angular-oauth2-oidc';
import {Router} from '@angular/router';
import {HttpBackend, HttpClient} from '@angular/common/http';
import {BehaviorSubject, combineLatest, Observable, ReplaySubject} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {UserInfoService} from '../services/user-info.service';
import {ENVIRONMENT, Environment} from 'src/app/services/environment';


interface FrontEndSecurityConfig {
  issuerUrl: string;
  clientId: string;
  scope: string;
  redirectUri?: string | null;
  enabled: boolean;
  requireLoginOverHttps: boolean;
  accountManagementUrl: string | null;
  orgManagementUrl: string | null;
}

@Injectable()
export class AuthService {
  isAuthenticatedSubject$ = new BehaviorSubject<boolean>(false);
  isDoneLoadingSubject$ = new ReplaySubject<boolean>();

  isAuthenticated$ = this.isAuthenticatedSubject$.asObservable();
  isDoneLoading$ = this.isDoneLoadingSubject$.asObservable();

  /**
   * Publishes `true` if and only if (a) all the asynchronous initial
   * login calls have completed or errorred, and (b) the user ended up
   * being authenticated.
   *
   * In essence, it combines:
   *
   * - the latest known state of whether the user is authorized
   * - whether the ajax calls for initial log in have all been done
   */
  canActivateProtectedRoutes$ = combineLatest([
    this.isAuthenticated$,
    this.isDoneLoading$
  ]).pipe(map(values => values.every(b => b)));


  private http: HttpClient;
  private _securityConfig: FrontEndSecurityConfig;

  get securityConfig():FrontEndSecurityConfig {
    return this._securityConfig;
  }


  constructor(
    @Inject(ENVIRONMENT) private environment: Environment,
    private oauthService: OAuthService,
    private router: Router,
    private userInfoService: UserInfoService,
    httpBackend: HttpBackend) {
    this.http = new HttpClient(httpBackend);

  }

  async bootstrapAuthService(): Promise<void> {
    try {
      const oauthEvent = await this.configureOAuthService();
      if (oauthEvent === true) {
        const loginResult = await this.oauthService.loadDiscoveryDocumentAndLogin({
          disableNonceCheck: true
        });
        console.log('Login result: ' + loginResult);
        if (!loginResult) {
          return Promise.reject('Login failed')
        }
        // const userProfile = await this.oauthService.loadUserProfile()
        // console.log('User:' , userProfile);

        //Open Idp is setup.
        this.isDoneLoadingSubject$.next(true);
        this.isAuthenticatedSubject$.next(true);

        await this.userInfoService.getUserInfo(true, this.oauthService.getAccessToken()).toPromise();
        this.oauthService.setupAutomaticSilentRefresh();
        this.router.initialNavigation();
      } else {
        await this.userInfoService.getUserInfo(true).toPromise();
        this.isDoneLoadingSubject$.next(true);
        this.isAuthenticatedSubject$.next(true);
      }
    } catch (e) {
      throw e;
    }
  }

  async logout(): Promise<void> {
    await this.oauthService.revokeTokenAndLogout();
  }

  private setupOpenIdpEventSubscriptions(): void {
    this.oauthService.events.subscribe(_ => {
      this.isAuthenticatedSubject$.next(this.oauthService.hasValidAccessToken());
    });


    this.oauthService.events
      .pipe(filter(e => ['token_received'].includes(e.type)))
      .subscribe(async e => {
        console.log(`token_received event ${this.oauthService.getAccessToken()}`);
        await this.userInfoService.getUserInfo(true, this.oauthService.getAccessToken()).toPromise();
      });


    this.oauthService.events
      .pipe(filter(e => ['session_terminated', 'session_error'].includes(e.type)))
      .subscribe(() => this.router.initialNavigation());
  }


  private async configureOAuthService(): Promise<boolean | null> {
    const authConfig: AuthConfig = await this.buildAuthConfig();
    if (authConfig != null) {
      this.oauthService.configure(authConfig);
      this.setupOpenIdpEventSubscriptions();
      // return this.runInitialLoginSequence2()
      return Promise.resolve(true);
      //return await this.runInitialLoginSequence();
    } else {
      return false;
    }
  }

  private async buildAuthConfig(): Promise<AuthConfig | null> {
    const securityConfig = await this.loadFrontendConfig().toPromise();
    this._securityConfig = securityConfig;
    if (!securityConfig.enabled) {
      return null;
    }

    const currentLocation = window.location.origin;
    // console.log(`current silent refresh => ${currentLocation}${slashIfNeeded}silent-refresh.html`);

    return new AuthConfig({
      issuer: securityConfig.issuerUrl,
      clientId: securityConfig.clientId,
      scope: securityConfig.scope,
      responseType: 'code',
      redirectUri: securityConfig.redirectUri || currentLocation,
      requireHttps: securityConfig.requireLoginOverHttps,

      clearHashAfterLogin: false,
      strictDiscoveryDocumentValidation: false,
      showDebugInformation: true,

    });
  }

  private loadFrontendConfig(): Observable<FrontEndSecurityConfig> {
    return this.http.get<FrontEndSecurityConfig>(`${this.environment.serverUrl}/api/security/config`);
  }


}

export function isAngularRouteHash(): boolean {
  const hash = window.location.hash;
  return hash.startsWith('#/') || hash.startsWith('#%2F');
}
