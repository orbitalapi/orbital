import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {taxiLangDef} from './taxi-lang-def';
import {ParsedSource, SourceCompilationError, VersionedSource} from '../services/schema';
import {editor, MarkerSeverity} from 'monaco-editor';
import {taxiLanguageConfiguration, taxiLanguageTokenProvider} from './taxi-lang.monaco';
import {MonacoEditorComponent, MonacoEditorLoaderService} from '@materia-ui/ngx-monaco-editor';
import {filter, take} from 'rxjs/operators';
import IMarkerData = editor.IMarkerData;
import ITextModel = editor.ITextModel;

declare const require: any;
declare const monaco: any; // monaco
/* tslint:disable-next-line */
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
  selectedSourceErrors: SourceCompilationError[];

  @ViewChild(MonacoEditorComponent)
  editor: MonacoEditorComponent;

  selectedIndex = 0;
  editorOptions: { theme: 'vs-dark', language: 'taxi' };

  monacoModel: ITextModel;

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

  constructor(private monacoLoaderService: MonacoEditorLoaderService) {
    this.monacoLoaderService.isMonacoLoaded.pipe(
      filter(isLoaded => isLoaded),
      take(1),
    ).subscribe(() => {
      monaco.editor.onDidCreateEditor(editorInstance => {
        editorInstance.updateOptions({readOnly: true});
      });
      monaco.editor.onDidCreateModel(model => {
        this.monacoModel = model;
        monaco.editor.defineTheme('vyne', {
          base: 'vs-dark',
          inherit: true,
          rules: [
            {token: '', background: '#333f54'},
          ],
          colors: {
            ['editorBackground']: '#333f54',
          }
        });
        monaco.editor.setTheme('vyne');
        monaco.editor.setModelLanguage(model, 'taxi');
      });
      monaco.languages.register({id: 'taxi'});
      monaco.languages.setLanguageConfiguration('taxi', taxiLanguageConfiguration);
      monaco.languages.setMonarchTokensProvider('taxi', taxiLanguageTokenProvider);
      // here, we retrieve monaco-editor instance
    });
  }

  // editorModel: NgxEditorModel;

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
    // this.editorModel = {
    //   value: this.selectedSource.source.content,
    //   language: 'typescript',
    //   uri: monaco.Uri.parse(`http://${this.selectedSource.source.name}`)// https://github.com/atularen/ngx-monaco-editor/issues/128
    // } as NgxEditorModel;
    const markers = this.selectedSourceErrors.map(error => {
      return {
        severity: MarkerSeverity.Error,
        message: error.detailMessage,
        startLineNumber: error.line,
        startColumn: error.char,
        endLineNumber: error.line,
        endColumn: error.char
      } as IMarkerData;
    });
    // if (this.monacoModel) {
    monaco.editor.setModelMarkers(
      this.monacoModel,
      'owner', // not sure what to pass here
      markers);
    // }
  }

  onInit($event: any) {
    console.log('Editor ready');
    this.monacoModel = $event.getModel();
  }
}

export type SidebarMode = 'Visible' | 'Hidden' | 'Auto';
