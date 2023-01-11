import { Environment } from 'src/app/services/environment';

export const environment:Environment = {
  production: true,
  // Convention for relative urls:  Start with an /, but don't end with one
  serverUrl: `//${window.location.host}`,
};
