
// Allows compile time switching of config (eg., routes, etc),
// based on if we're build Orbital or Vyne.
// To use this, import app-instance.vyne, and then switch on appInstanceType.
// The value is switched at compile time for Orbital (configured in angular.json).
//
// eg: (in a route...)
// component: appInstanceType.appType == 'vyne' ? SchemaExplorerContainerComponent : OrbitalSchemaExplorerContainerComponent
import { AppType } from 'src/app/app-config/app-type';

export const appInstanceType: { appType: AppType } = {
  appType:  'vyne'
}
