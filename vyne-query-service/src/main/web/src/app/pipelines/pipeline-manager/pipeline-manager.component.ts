import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-pipeline-manager',
  template: `<app-header-bar title="Pipeline manager">
    <button mat-stroked-button (click)="createNewPipeline()">Create new pipeline</button>
  </app-header-bar>
  <div class="page-content">
    <router-outlet></router-outlet>
  </div>`,
  styleUrls: ['./pipeline-manager.component.scss']
})
export class PipelineManagerComponent {

  constructor(private router: Router, private activatedRoute: ActivatedRoute) {
  }

  createNewPipeline() {
    this.router.navigate(['new'], {relativeTo: this.activatedRoute})
  }

}
