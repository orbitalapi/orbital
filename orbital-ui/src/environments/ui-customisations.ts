import {AppConfig} from "../app/services/app-info.service";
import {SidebarElement} from '../app/sidenav/sidenav.component';


/**
 * A list of places in the docs we link to.
 * Allows whitelabels to provide analagous links in their own docs
 */
export class DocsLinks {
  constructor(
    public readonly configureMetricsReporting = 'https://orbitalhq.com/docs/querying/observability#configuring-prometheus',
    public readonly publishQueriesAsEndpoints = 'https://orbitalhq.com/docs/querying/queries-as-endpoints'
  ) {
  }
}

export const UiCustomisations = {
  landingPageWelcomeText: 'Welcome to Orbital',
  productName: 'Orbital',
  customSidebarElements: function (appConfig: AppConfig): SidebarElement[] {
    return [];
  },
  docsLinks: new DocsLinks()
}
