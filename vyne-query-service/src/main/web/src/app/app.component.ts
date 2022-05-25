import { Component } from '@angular/core';
import { TypesService } from './services/types.service';
import { AppInfoService } from './services/app-info.service';

@Component({
  selector: 'app-root',
  template: `
    <tui-root>
      <vyne-app></vyne-app>
    </tui-root>
  `,
  providers: [TypesService, AppInfoService]
})
export class AppComponent {
}
