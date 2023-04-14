import {Component, Inject} from '@angular/core';
import {AppInfoService} from './services/app-info.service';
import {TUI_DIALOGS} from "@taiga-ui/cdk";
import {combineLatest, Observable} from "rxjs";
import {map} from "rxjs/operators";

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
