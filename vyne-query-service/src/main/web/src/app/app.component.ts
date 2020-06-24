import {Component} from '@angular/core';
import {TypesService} from './services/types.service';
import {AppInfo, AppInfoService} from './services/app-info.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  providers: [TypesService, AppInfoService]
})
export class AppComponent {
  title = 'vyne-app';
}
