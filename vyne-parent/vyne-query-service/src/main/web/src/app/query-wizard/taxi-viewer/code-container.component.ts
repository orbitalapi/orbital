import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-code-container',
  styleUrls: ['./code-container.component.scss'],
  preserveWhitespaces: true,
  template: `
    <div class="code-container">
      <div class="title-bar" (click)="toggleVisibility()">
        <span class="language-name">{{ name }}</span>
        <div class="spacer"></div>
        <i class="material-icons" [ngClass]="expandWrapperClass">expand_less</i>
      </div>
      <div class="expand-wrapper" [ngClass]="expandWrapperClass">
        <td-highlight [lang]="lang" [content]="_content" *ngIf="hasContent">
        </td-highlight>
      </div>
    </div>
  `
})
export class CodeContainerComponent {

  _content: string;

  @Input()
  name: string;

  get content(): any {
    return this._content;
  }

  @Input()
  set content(value: any) {
    if (typeof value === "string") {
      this._content = value
    } else {
      this._content = JSON.stringify(value, null, 2);
    }
  }

  @Input()
  lang: string = "";

  @Input()
  expanded: boolean = true;

  get expansionIcon(): string {
    return (this.expanded) ? "expand_less" : "expand_more"
  }

  get hasContent(): boolean {
    return this.content !== "" && this.lang !== "";
  }

  get expandWrapperClass(): string {
    return (this.expanded) ? "panel-visible" : "panel-hidden";
  }

  toggleVisibility() {
    this.expanded = !this.expanded;
  }

}
