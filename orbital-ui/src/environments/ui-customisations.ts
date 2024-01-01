import {AppConfig} from "../app/services/app-info.service";
import {SidebarElement} from "../app/vyne/vyne.component";

export const UiCustomisations = {
  landingPageWelcomeText: 'Welcome to Orbital',
  customSidebarElements: function (appConfig: AppConfig): SidebarElement[] {
    return [];
  }
}
