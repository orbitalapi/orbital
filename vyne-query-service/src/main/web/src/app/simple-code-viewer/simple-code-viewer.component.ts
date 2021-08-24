import {Component, Input} from '@angular/core';
import {HighlightJS} from 'ngx-highlightjs';
import {taxiLangDef} from '../code-viewer/taxi-lang-def';

@Component({
  selector: 'app-simple-code-viewer',
  styleUrls: ['./simple-code-viewer.component.scss'],
  preserveWhitespaces: true,
  template: `
    <div class="code-container">
      <div class="title-bar" (click)="toggleVisibility()">
        <span class="language-name">{{ name }}</span>
        <div class="spacer"></div>
        <i class="material-icons" [ngClass]="expandWrapperClass">expand_less</i>
      </div>
      <div class="expand-wrapper hljs" [ngClass]="expandWrapperClass">
        <pre><code [highlight]="_content" [languages]="[lang]"></code></pre>
        <!--        <td-highlight [lang]="lang" [content]="_content" *ngIf="hasContent">-->
        <!--        </td-highlight>-->
      </div>
    </div>
  `
})
export class SimpleCodeViewerComponent {

  constructor(private higlightJs: HighlightJS) {
    higlightJs.registerLanguage('taxi', taxiLangDef as any);
  }

  _content: string;

  @Input()
  name: string;

  get content(): any {
    return this._content;
  }

  @Input()
  set content(value: any) {
    if (typeof value === 'string') {
      this._content = value;
    } else {
      this._content = JSON.stringify(value, null, 2);
    }
  }

  @Input()
  lang = '';

  @Input()
  expanded = true;

  get expansionIcon(): string {
    return (this.expanded) ? 'expand_less' : 'expand_more';
  }

  get hasContent(): boolean {
    return this.content !== '' && this.lang !== '';
  }

  get expandWrapperClass(): string {
    return (this.expanded) ? 'panel-visible' : 'panel-hidden';
  }

  toggleVisibility() {
    this.expanded = !this.expanded;
  }

}
