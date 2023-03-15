import { Environment } from 'src/app/services/environment';

export const environment: Environment = {
  production: false,
  // Convention for relative urls:  Start with an /, but don't end with one
  serverUrl: `//localhost:4200`,
};
