import {Component, Input, OnInit} from '@angular/core';
import {taxiLangDef} from './taxi-lang-def';
import {SourceCode} from "../services/schema";

declare const require: any;
/* tslint:disable-next-line */
let hljs: any = require('highlight.js/lib');
hljs.registerLanguage('taxi', taxiLangDef);

@Component({
  selector: 'app-code-viewer',
  templateUrl: './code-viewer.component.html',
  styleUrls: ['./code-viewer.component.scss']
})
export class CodeViewerComponent {

  @Input()
  sources: SourceCode[];

  @Input()
  sidebarMode: SidebarMode = 'Auto';

  get displaySidebar(): boolean {
    switch (this.sidebarMode) {
      case 'Auto':
        return this.sources && this.sources.length > 1;
      case 'Visible':
        return true;
      case 'Hidden' :
        return false;
    }
  }
}

export type SidebarMode = 'Visible' | 'Hidden' | 'Auto';
