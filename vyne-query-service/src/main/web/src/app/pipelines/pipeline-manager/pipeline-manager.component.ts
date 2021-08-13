import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
  selector: 'app-pipeline-manager',
  templateUrl: './pipeline-manager.component.html',
  styleUrls: ['./pipeline-manager.component.scss']
})
export class PipelineManagerComponent {

  constructor(private router: Router, private activatedRoute: ActivatedRoute) {
  }

  createNewPipeline() {
    this.router.navigate(['new'], {relativeTo: this.activatedRoute})
  }

}
