import {Component, Input, OnInit} from '@angular/core';
import {Contents} from "../toc-host.directive";

@Component({
  selector: 'app-contents-table',
  template: `
    <div class="contents-table" *ngIf="contents">
      <h4>On this page</h4>
      <ul class="list-reset">
        <li *ngFor="let contentsItem of contents.items">
          <a [href]="asLink(contentsItem.slug)">{{contentsItem.name}}</a>
        </li>
      </ul>
    </div>
  `,
  styleUrls: ['./contents-table.component.scss']
})
export class ContentsTableComponent {

  @Input()
  contents: Contents;

  asLink(slug: string): string {
    return '#' + slug;
  }
}
