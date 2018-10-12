import {Component} from '@angular/core';
import {TypesService} from "./services/types.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  providers: [TypesService]
})
export class AppComponent {
  title = 'vyne-app';
}
