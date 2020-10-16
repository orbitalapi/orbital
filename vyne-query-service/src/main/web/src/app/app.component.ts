import {Component} from '@angular/core';
import {TypesService} from './services/types.service';
import {AppInfo, AppInfoService} from './services/app-info.service';

@Component({
  selector: 'app-root',
  template: `<vyne-app></vyne-app>`,
  providers: [TypesService, AppInfoService]
})
export class AppComponent {
  title = 'vyne-app';
}
