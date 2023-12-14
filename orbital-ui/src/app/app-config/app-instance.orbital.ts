import { AppType } from 'src/app/app-config/app-type';

// See app-instance.vyne for what this is.
//
// IMPORTANT: Don't import this type.  ALWAYS import app-instance.vyne
// Swapping is handled in angular.json, to ensure that orbital gets the
// correct config at compile time.
export const appInstanceType: { appType: AppType } = {
  appType:  'orbital'
}
