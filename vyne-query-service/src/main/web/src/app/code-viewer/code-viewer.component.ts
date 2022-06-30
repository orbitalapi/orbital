import { Component, Input } from '@angular/core';
import { taxiLangDef } from './taxi-lang-def';
import { CompilationMessage, ParsedSource, VersionedSource } from '../services/schema';
import { editor } from 'monaco-editor';

declare const require: any;
/* eslint-disable-next-line */
let hljs: any = require('highlight.js/lib');
hljs.registerLanguage('taxi', taxiLangDef);

@Component({
  selector: 'app-code-viewer',
  templateUrl: './code-viewer.component.html',
  styleUrls: ['./code-viewer.component.scss']
})
export class CodeViewerComponent {

  private _sources: ParsedSource[] | VersionedSource[];

  @Input()
  get sources(): ParsedSource[] | VersionedSource[] {
    return this._sources;
  }

  set sources(value: ParsedSource[] | VersionedSource[]) {
    this._sources = value;
    if (this.sources && (this.sources as any[]).length > 0) {
      this.select(this.sources[0]);
    }
  }

  @Input()
  sidebarMode: SidebarMode = 'Auto';

  selectedSource: VersionedSource;
  selectedSourceErrors: CompilationMessage[];

  selectedIndex = 0;

  private static isVersionedSource(source: ParsedSource | VersionedSource): source is VersionedSource {
    if (!source) {
      return false;
    }
    const isParsedSource = (source as ParsedSource).source !== undefined && (source as ParsedSource).errors !== undefined;
    return !isParsedSource;
  }

  private static versionedSource(input: ParsedSource | VersionedSource): VersionedSource {
    if (CodeViewerComponent.isVersionedSource(input)) {
      return input;
    } else {
      return (input as ParsedSource).source;
    }
  }

  constructor() {
  }


  get displaySidebar(): boolean {
    switch (this.sidebarMode) {
      case 'Auto':
        return this._sources && this._sources.length > 1;
      case 'Visible':
        return true;
      case 'Hidden' :
        return false;
    }
  }

  get sourceContent(): string {
    if (!this.selectedSource) {
      return '';
    } else {
      return this.selectedSource.content;
    }
  }

  select(source: ParsedSource | VersionedSource) {
    this.selectedIndex = (this._sources as any[]).indexOf(source);
    this.selectedSource = CodeViewerComponent.versionedSource(source);
    this.selectedSourceErrors = (source as ParsedSource).errors || [];
  }

}

export type SidebarMode = 'Visible' | 'Hidden' | 'Auto';
