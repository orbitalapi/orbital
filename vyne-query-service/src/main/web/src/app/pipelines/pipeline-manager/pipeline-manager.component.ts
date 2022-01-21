import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-pipeline-manager',
  template: `
    <app-header-bar title="Pipeline manager">
    </app-header-bar>
    <div class="page-content centered-page-block-container">
      <div class="centered-page-block">
        <router-outlet></router-outlet>
      </div>
    </div>`,
  styleUrls: ['./pipeline-manager.component.scss']
})
export class PipelineManagerComponent {


}
