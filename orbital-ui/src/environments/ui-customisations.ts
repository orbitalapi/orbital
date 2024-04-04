import { AppConfig } from "../app/services/app-info.service";
import { SidebarElement } from '../app/sidenav/sidenav.component';

export const UiCustomisations = {
  landingPageWelcomeText: 'Welcome to Orbital',
  productName: 'Orbital',
  customSidebarElements: function (appConfig: AppConfig): SidebarElement[] {
    return [];
  }
}
