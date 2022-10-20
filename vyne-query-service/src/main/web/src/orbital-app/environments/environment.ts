// This file can be replaced during build by using the `fileReplacements` array.
// `ng build ---prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

import { Environment } from 'src/app/services/environment';

export const environment: Environment = {
  production: false,
  // Convention for relative urls:  Start with an /, but don't end with one
  serverUrl: `//localhost:4200`,
}
