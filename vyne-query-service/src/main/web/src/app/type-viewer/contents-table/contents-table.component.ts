import {Component, Input} from '@angular/core';
import {Contents} from "../toc-host.directive";
import {ActivatedRoute, Router} from "@angular/router";
import {timeout} from "rxjs/operators";


@Component({
  selector: 'app-contents-table',
  template: `
    <div class="contents-table" *ngIf="contents">
      <h4>On this page</h4>
      <ul class="list-reset">
        <li *ngFor="let contentsItem of contents.items">
          <a href="javascript: void(0);" (click)="scroll(contentsItem.slug)">{{contentsItem.name}}</a>
        </li>
      </ul>
    </div>
  `,
  styleUrls: ['./contents-table.component.scss']
})
export class ContentsTableComponent {
  public href: string = "";

  constructor(private router: Router) {
  }

  @Input()
  contents: Contents;

  ngOnInit() {
    this.href = this.router.url;
  }

  /*
  TODO: The function below was added to bypass the problem of scrolling.
   Unless we don't use setTimeOut here, scrolling only runs properly on the second click.
   It should be checked again after Angular 9 update.
  */

  scroll(slug: string) {
    window.location.href = this.href + '#' + slug;
    setTimeout(() => {
      document.querySelector('#' + slug).scrollIntoView()
    }, 0);
  }
}
