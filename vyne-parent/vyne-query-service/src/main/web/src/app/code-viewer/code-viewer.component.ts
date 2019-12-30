import {Component, Input, OnInit} from '@angular/core';
import {taxiLangDef} from './taxi-lang-def';

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
  sources: Source[];
}

export interface Source {
  name: string;
  code: string;
  language: string;
}
