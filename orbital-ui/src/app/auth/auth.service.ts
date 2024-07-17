import {Inject, Injectable} from '@angular/core';
import {AuthConfig, OAuthService} from 'angular-oauth2-oidc';
import {Router} from '@angular/router';
import {HttpBackend, HttpClient} from '@angular/common/http';
import {BehaviorSubject, combineLatest, lastValueFrom, Observable, ReplaySubject} from 'rxjs';
import {filter, map, tap} from 'rxjs/operators';
import {UserInfoService} from '../services/user-info.service';
import {ENVIRONMENT, Environment} from 'src/app/services/environment';

interface FrontEndSecurityConfig {
  issuerUrl: string;
  oidcDiscoveryUrl: string;
  clientId: string;
  scope: string;

  redirectUri?: string | null;
  enabled: boolean;
  requireLoginOverHttps: boolean;
  refreshTokensDisabled: boolean;
  accountManagementUrl: string | null;
  orgManagementUrl: string | null;
  identityTokenKind: 'Access' | 'Id';
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
        const loginResult = await this.loadDiscoveryDocumentAndLogin();
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
      console.log("error in bootstrapping Auth Service", e)
      throw e;
    }
  }

  /**
   * Wrapper around this.oauthService.loadDiscoveryDocumentAndLogin
   * which considers oidcDiscoveryUrl
   */
  async loadDiscoveryDocumentAndLogin() {
    let loginOptions = {
      disableNonceCheck: true
    };

    // This is the "normal" use-case ...
    // The IDP follows OpenIDConnect conventions, so we can just
    // load the discovery document from the convential url.
    // Our OAuth library makes this use-case easy, so just call the method.
    if (!this.securityConfig.oidcDiscoveryUrl) {
      try {
        return await this.oauthService.loadDiscoveryDocumentAndLogin(loginOptions);
      } catch (e) {
        console.error("Error in loading discovery document and login");
        console.error(e);
        // angular-oauth2-oidc library uses browser's session store to persist various OIDC data
        // including PKCE_Verifier. However, session store implementation various between browsers and Chrome's
        // pre-fetch feature might lead corruptions in its session store.
        // see https://issues.chromium.org/issues/40940701 for details.
        // When that happens PKCE_Verifier can be removed from the session store causing 'fetch token' calls to fail.
        // Re-initialising the whole login flow seems to be the only feasible way to recover from this.
        this.oauthService.initLoginFlow();
        return false;
      }
    }

    // The "exceptional" use-case is where the discoveryDocument lives at a non-conventional
    // url.
    // This isn't a big problem, but the library doesn't expose convenience methods for this flow,
    // so we have to implement the loadDiscoveryDocumentAndLogin() method ourselves.
    // The library internally is doing the following:
    //  - loadDiscoveryDocumentAndLogin()
    //    - calls loadDiscoveryDocument(url)
    //      - calls tryLogin()
    //  - then validation logic
    //  - then call initLoginFlow.
    //  -
    const loginResult = await this.oauthService.loadDiscoveryDocument(this.securityConfig.oidcDiscoveryUrl).then(() => {
      return this.oauthService.tryLogin(loginOptions);
    });

    // From @angular-oauth2-oidc/oauth-service loadDiscoveryDocumentAndLogin:
    if (!this.oauthService.hasValidIdToken() || !this.oauthService.hasValidAccessToken()) {
      this.oauthService.initLoginFlow();
      return false;
    } else {
      return true;
    }
  }

  async doSilentRefresh(): Promise<void> {
    await this.oauthService.silentRefresh();
  }

  tokenEndPoint(): string {
    return this.oauthService.tokenEndpoint;
  }

  async logoutOidc(): Promise<void> {
    await this.oauthService.revokeTokenAndLogout();
  }

  async samlLogout(): Promise<void> {
    const source = this.http.get(`${this.environment.serverUrl}/api/saml/logout`);
    await lastValueFrom(source);
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

    this.oauthService.events
      .pipe(
        tap(e => console.log(e.type)),
        filter(e => ['silent_refresh_timeout'].includes(e.type)))
      .subscribe(() => {
        //this.oauthService.initLoginFlow()
      });
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
    const slashIfNeeded = currentLocation.endsWith('/') ? '' : '/';
    if (securityConfig.refreshTokensDisabled) {
      console.log(`current silent refresh => ${currentLocation}${slashIfNeeded}silent-refresh.html`);
    }

    return new AuthConfig({
      issuer: securityConfig.issuerUrl,
      clientId: securityConfig.clientId,
      scope: securityConfig.scope,
      responseType: 'code',
      redirectUri: securityConfig.redirectUri || currentLocation,
      requireHttps: securityConfig.requireLoginOverHttps,
      useSilentRefresh: securityConfig.refreshTokensDisabled,
      //silentRefreshRedirectUri: `${currentLocation}${slashIfNeeded}silent-refresh.html`,
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
