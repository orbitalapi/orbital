import { Component } from '@angular/core';
import { AppInfoService } from './services/app-info.service';

@Component({
  selector: 'app-root',
  template: `
    <tui-root>
      <vyne-app></vyne-app>
    </tui-root>
  `,
  providers: [AppInfoService],
})
export class AppComponent {
}
