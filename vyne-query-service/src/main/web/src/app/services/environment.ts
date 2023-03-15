/**
 * A common interface for sharing envs between Vyne and Orbital.
 * Don't import env, instead inject Environment into your service
 * eg:
 *
 * Instead of:
 *
 * import { environment } from '../../environments/environment';
 *
 * Use:
 *
 * import { ENVIRONMENT, Environment } from 'src/app/services/environment';
 *
 * constructor(
 *     @Inject(ENVIRONMENT) private environment: Environment,
 *     ...
 * )
 *
 * Then replace http.get(`${environment.serverUrl}`)
 *
 * with
 *
 * http.get(`${this.environment.serverUrl}`)
 */
import { InjectionToken } from '@angular/core';

export interface Environment {
  production: boolean;
  serverUrl: string;
}

export const ENVIRONMENT = new InjectionToken<Environment>('app.environment')
