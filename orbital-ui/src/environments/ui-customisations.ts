import {AppConfig} from "../app/services/app-info.service";
import {SidebarElement} from '../app/sidenav/sidenav.component';


export class DocsLinks {
  constructor(
    public readonly configureMetricsReporting = 'https://orbitalhq.com/docs/querying/observability#configuring-prometheus'
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
