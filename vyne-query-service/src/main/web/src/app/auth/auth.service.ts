import {Injectable} from '@angular/core';
import {AuthConfig, JwksValidationHandler, OAuthService} from 'angular-oauth2-oidc';
import {Router} from '@angular/router';
import {HttpBackend, HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable, ReplaySubject, combineLatest} from 'rxjs';
import {environment} from '../../environments/environment';
import { filter, map } from 'rxjs/operators';
import {UserInfoService} from "../services/user-info.service";



interface FrontendConfig {
  issuerUrl: string;
  clientId: string;
  scope: string;
  enabled: boolean;
}

@Injectable()
export class AuthService {

  private http: HttpClient;

  private errorDuringBootstrap: any = undefined;

  private isAuthenticatedSubject$ = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject$.asObservable();

  private isDoneLoadingSubject$ = new ReplaySubject<boolean>();
  public isDoneLoading$ = this.isDoneLoadingSubject$.asObservable();


  constructor(private oauthService: OAuthService,
              private router: Router,
              private userInfoService: UserInfoService,
              httpBackend: HttpBackend) {
    this.http = new HttpClient(httpBackend);

  }

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
  public canActivateProtectedRoutes$: Observable<boolean> = combineLatest([
    this.isAuthenticated$,
    this.isDoneLoading$
  ]).pipe(map(values => values.every(b => b)));

  async bootstrapAuthService(): Promise<void> {
    try {
      const oauthEvent = await this.configureOAuthService();
      if (oauthEvent === true) {
        //Open Idp is setup.
        await this.userInfoService.getUserInfo(true, this.oauthService.getAccessToken()).toPromise();
        this.oauthService.setupAutomaticSilentRefresh();
        this.router.initialNavigation();
      } else {
        await this.userInfoService.getUserInfo(true).toPromise();
        this.isDoneLoadingSubject$.next(true);
        this.isAuthenticatedSubject$.next(true);
      }
    } catch (e) {
      this.errorDuringBootstrap = e;
      throw e;
    }
  }

  private setUpOpendIdpEventSubscriptions() {
    this.oauthService.events.subscribe(_ => {
      this.isAuthenticatedSubject$.next(this.oauthService.hasValidAccessToken());
    });


    this.oauthService.events
      .pipe(filter(e => ['token_received'].includes(e.type)))
      .subscribe(e =>  this.userInfoService.getUserInfo(true, this.oauthService.getAccessToken()));


    this.oauthService.events
      .pipe(filter(e => ['session_terminated', 'session_error'].includes(e.type)))
      .subscribe(e => this.router.initialNavigation());
  }

  public get bootstrapError(): any {
    return this.errorDuringBootstrap;
  }

  public logout() {
    this.oauthService.revokeTokenAndLogout();
    this.oauthService.logOut();
  }

  // Start 'Authorization Code Flow' see https://tools.ietf.org/html/rfc6749#section-1.3.1
  private async startAuthorisationCodeFlow(): Promise<void> {
    const state = isAngularRouteHash() ? window.location.hash : '';
    this.oauthService.initCodeFlow(state);

    // Stop the boot process of the angular app as the user will be redirected to the auth provider by the above statement.
    await new Promise<void>(() => {
    });
  }

  private async configureOAuthService(): Promise<boolean | null> {
    const authConfig: AuthConfig = await this.buildAuthConfig();
    if (authConfig != null) {
      this.oauthService.configure(authConfig);
      this.setUpOpendIdpEventSubscriptions()
      this.oauthService.tokenValidationHandler = new JwksValidationHandler();
      return this.runInitialLoginSequence()
        .then(_ => Promise.resolve(true))
      //return await this.runInitialLoginSequence();
    } else {
      return false;
    }
  }

  private async buildAuthConfig(): Promise<AuthConfig | null> {
    const frontendConfig = await this.loadFrontendConfig().toPromise();
    if (!frontendConfig.enabled) {
      return null;
    }

    const currentLocation = window.location.origin;
    const slashIfNeeded = currentLocation.endsWith('/') ? '' : '/';

    return new AuthConfig({
      issuer: frontendConfig.issuerUrl,
      clientId: frontendConfig.clientId,
      scope: frontendConfig.scope,
      responseType: 'code',
      redirectUri: currentLocation,
      silentRefreshRedirectUri: `${currentLocation}${slashIfNeeded}silent-refresh.html`,
      clearHashAfterLogin: false,
      strictDiscoveryDocumentValidation: false,
      showDebugInformation: true,
      requireHttps: false
    });
  }

  private loadFrontendConfig(): Observable<FrontendConfig> {
    return this.http.get<FrontendConfig>(`${environment.queryServiceUrl}/api/security/config`);
  }

  private runInitialLoginSequence(): Promise<void> {
    // 0. LOAD CONFIG:
    // First we have to check to see how the IdServer is
    // currently configured:
    return this.oauthService.loadDiscoveryDocument()
      // 1. HASH LOGIN:
      // Try to log in via hash fragment after redirect back
      // from IdServer from initImplicitFlow:
      .then(() => this.oauthService.tryLogin())
      .then(() => {
        if (this.oauthService.hasValidAccessToken()) {
          return Promise.resolve();
        }
        // 2. SILENT LOGIN:
        // Try to log in via a refresh because then we can prevent
        // needing to redirect the user:
        return this.oauthService.silentRefresh()
          .then(() => Promise.resolve())
          .catch(result => {
            // Subset of situations from https://openid.net/specs/openid-connect-core-1_0.html#AuthError
            // Only the ones where it's reasonably sure that sending the
            // user to the IdServer will help.
            const errorResponsesRequiringUserInteraction = [
              'interaction_required',
              'login_required',
              'account_selection_required',
              'consent_required',
            ];

            if (result
              && result.reason.params
              && errorResponsesRequiringUserInteraction.indexOf(result.reason.params.error) >= 0) {

              // 3. ASK FOR LOGIN:
              // At this point we know for sure that we have to ask the
              // user to log in, so we redirect them to the IdServer to
              // enter credentials.
              //
              console.warn('User interaction is needed to log in, we will wait for the user to manually log in.');
              const state = isAngularRouteHash() ? window.location.hash : '';
              this.oauthService.initCodeFlow(state);
              return Promise.resolve();
            }

            // We can't handle the truth, just pass on the problem to the
            // next handler.
            return Promise.reject(result);
          });
      })

      .then(() => {
        this.isDoneLoadingSubject$.next(true);

        // Check for the strings 'undefined' and 'null' just to be sure. Our current
        // login(...) should never have this, but in case someone ever calls
        // initImplicitFlow(undefined | null) this could happen.
        if (this.oauthService.state && this.oauthService.state !== 'undefined' && this.oauthService.state !== 'null') {
          let stateUrl = this.oauthService.state;
          if (stateUrl.startsWith('/') === false) {
            stateUrl = decodeURIComponent(stateUrl);
          }
          console.log(`There was state of ${this.oauthService.state}, so we are sending you to: ${stateUrl}`);
          this.router.navigateByUrl(stateUrl);
        }
      })
      .catch((error) => { console.error(error); this.isDoneLoadingSubject$.next(true) });
  }
}

export function isAngularRouteHash(): boolean {
  const hash = window.location.hash;
  return hash.startsWith('#/') || hash.startsWith('#%2F');
}
