import {AppConfig} from "../app/services/app-info.service";
import {SidebarElement} from "../app/vyne/vyne.component";

export const UiCustomisations = {
  landingPageWelcomeText: 'Welcome to Orbital',
  productName: 'Hazelcast Connect',
  customSidebarElements: function (appConfig: AppConfig): SidebarElement[] {
    return [];
  }
}
