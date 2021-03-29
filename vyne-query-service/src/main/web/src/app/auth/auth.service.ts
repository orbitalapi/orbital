import {Injectable} from '@angular/core';
import {AuthConfig, JwksValidationHandler, OAuthService} from 'angular-oauth2-oidc';
import {Router} from '@angular/router';
import {HttpBackend, HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';
import {OAuthSuccessEvent} from "angular-oauth2-oidc/events";


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

  constructor(private oauthService: OAuthService,
              private router: Router,
              httpBackend: HttpBackend) {
    this.http = new HttpClient(httpBackend);
  }

  async bootstrapAuthService(): Promise<void> {
    try {
      const oauthEvent = await this.configureOAuthService();
      if (oauthEvent != null) {
        await this.tryLogin();
        if (!this.oauthService.hasValidAccessToken()) {
          await this.startAuthorisationCodeFlow();
        } else {
          this.oauthService.setupAutomaticSilentRefresh();

          this.router.initialNavigation();
        }
      }
    } catch (e) {
      this.errorDuringBootstrap = e;
      throw e;
    }
  }

  public get bootstrapError(): any {
    return this.errorDuringBootstrap;
  }

  public logout() {
    this.oauthService.logOut();
  }

  // Start 'Authorization Code Flow' see https://tools.ietf.org/html/rfc6749#section-1.3.1
  private async startAuthorisationCodeFlow(): Promise<void> {
    const state = isAngularRouteHash() ? window.location.hash : '';
    this.oauthService.initCodeFlow(state);

    // Stop the boot process of the angular app as the user will be redirected to the auth provider by the above statement.
    await new Promise<void>(() => {});
  }

  private async tryLogin() {
    await this.oauthService.tryLoginCodeFlow({
      onTokenReceived: info => {
        window.location.hash = info.state;
      },
      customHashFragment: isAngularRouteHash() ? '' : window.location.hash
    });
  }

  private async configureOAuthService(): Promise<OAuthSuccessEvent | null> {
    const authConfig: AuthConfig = await this.buildAuthConfig();
    if (authConfig != null) {
      this.oauthService.configure(authConfig);
      this.oauthService.tokenValidationHandler = new JwksValidationHandler();
      return await this.oauthService.loadDiscoveryDocument();
    } else {
      return null;
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
      strictDiscoveryDocumentValidation: false
    });
  }

  private loadFrontendConfig(): Observable<FrontendConfig> {
    return this.http.get<FrontendConfig>(`${environment.queryServiceUrl}/api/security/config`);
  }
}

export function isAngularRouteHash(): boolean {
  const hash = window.location.hash;
  return hash.startsWith('#/') || hash.startsWith('#%2F');
}
