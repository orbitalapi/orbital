import {Injectable, Optional, Provider} from '@angular/core';
import {OAuthModuleConfig, OAuthResourceServerErrorHandler, OAuthStorage} from 'angular-oauth2-oidc';
import {
  HTTP_INTERCEPTORS,
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest, HttpStatusCode
} from '@angular/common/http';
import {Observable} from 'rxjs';
import {catchError} from "rxjs/operators";
import {AuthService} from "./auth.service";


@Injectable()
export class DefaultOAuthInterceptor implements HttpInterceptor {

  constructor(
    private authStorage: OAuthStorage,
    private errorHandler: OAuthResourceServerErrorHandler,
    @Optional() private moduleConfig: OAuthModuleConfig,
    private authService: AuthService
  ) {}

  private checkUrl(url: string): boolean {
    let found = this.moduleConfig.resourceServer.allowedUrls.find(u => url.startsWith(u));
    return !!found;
  }

  public intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

    let url = req.url.toLowerCase();

    if (!this.moduleConfig) return next.handle(req);
    if (!this.moduleConfig.resourceServer) return next.handle(req);
    if (!this.moduleConfig.resourceServer.allowedUrls) return next.handle(req);
    if (!this.checkUrl(url)) return next.handle(req);
    if (this.authService.securityConfig.enabled === false) return next.handle(req);
    // Allow the server to define which type of token we should pass in the authorization header.
    // The default our auth library uses is "access". However, for some OIDC providers, the information
    // we need is actually in the Id token.
    // (Both are normally provided)
    const identityTokenKind = this.authService.securityConfig.identityTokenKind;
    let token = identityTokenKind === 'Id' ? this.authStorage.getItem('id_token') : this.authStorage.getItem('access_token')

    let header = 'Bearer ' + token;

    let headers = req.headers
      .set('Authorization', header);

    req = req.clone({headers});

    return next.handle(req)
      .pipe(
        catchError(err => {
             console.log(this.authService.tokenEndPoint())
             if (err instanceof HttpErrorResponse && err.status === HttpStatusCode.Unauthorized) {
               console.log("doing silent refresh");
               return this.authService.doSilentRefresh();
             }
             return this.errorHandler.handleError(err);
            }
        )
      );

  }

}

export const authenticationInterceptor: Provider =
  {provide: HTTP_INTERCEPTORS, useClass: DefaultOAuthInterceptor, multi: true};
