import {Component, Input} from '@angular/core';
import {ProfilerOperation} from "../services/query.service";

@Component({
  selector: 'app-profile-viewer',
  templateUrl: './profile-viewer.component.html',
  styleUrls: ['./profile-viewer.component.scss']
})
export class ProfileViewerComponent {


  @Input()
  profilerOperation: ProfilerOperation

}
