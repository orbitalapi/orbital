import { Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { editor } from 'monaco-editor';


// Import the core monaco editor
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';
import * as monadoEditorAll from 'monaco-editor/esm/vs/editor/editor.all.js';
import * as languageFeatureService from 'monaco-editor/esm/vs/editor/common/services/languageFeaturesService.js';

// Import features we care abut
import * as monacoFeature4
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickAccess/standaloneHelpQuickAccess.js';
import * as monacoFeature5
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickAccess/standaloneGotoLineQuickAccess.js';
import * as monacoFeature6
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickAccess/standaloneGotoSymbolQuickAccess.js';
import * as monacoFeature7
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickAccess/standaloneCommandsQuickAccess.js';
import * as monacoFeature8
  from 'monaco-editor/esm/vs/editor/standalone/browser/quickInput/standaloneQuickInputService.js';
import { JSONPathFinder } from 'src/app/json-viewer/JsonPathFinder';
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import ITextModel = editor.ITextModel;

@Component({
  selector: 'app-json-viewer',
  template: `
    <div #codeEditorContainer class="code-editor"></div>`,
  styleUrls: ['./json-viewer.component.scss']
})
export class JsonViewerComponent implements OnDestroy {

  constructor() {
    // This does nothing, but prevents tree-shaking
    const features = [monadoEditorAll, monacoFeature4, monacoFeature5, monacoFeature6, monacoFeature7, monacoFeature8, languageFeatureService];

  }

  private _json: string;
  private pathFinder: JSONPathFinder

  @Input()
  get json(): string {
    return this._json;
  }

  set json(value: string) {
    if (value === this._json) {
      return;
    }
    this._json = value;
    this.pathFinder = new JSONPathFinder(this.json)
    this.createOrUpdateEditor()
  }

  private monacoEditor: IStandaloneCodeEditor;
  private _codeEditorContainer: ElementRef;
  @ViewChild('codeEditorContainer')
  get codeEditorContainer(): ElementRef {
    return this._codeEditorContainer;
  }

  set codeEditorContainer(value: ElementRef) {
    this._codeEditorContainer = value;
    this.createOrUpdateEditor()
  }

  ngOnDestroy(): void {
    if (this.monacoEditor) {
      this.monacoEditor.getModel().dispose();
      this.monacoEditor.dispose();
    }
  }

  private createOrUpdateEditor(): void {
    if (!this.codeEditorContainer) {
      return;
    }
    if (!this.json) {
      return;
    }
    if (this.monacoEditor) {
      this.monacoEditor.getModel().setValue(this.json);
    } else {
      let monacoTextModel: ITextModel
      if (this.json) {
        const modelUri = monaco.Uri.parse('inmemory://dummy.json'); // a made up unique URI for our model
        monacoTextModel = monaco.editor.createModel(this.json, 'json', modelUri)
      }

      this.monacoEditor = monaco.editor.create(this._codeEditorContainer.nativeElement, {
        model: monacoTextModel,
        glyphMargin: true,
        automaticLayout: true,
        readOnly: false,
        folding: true,
      });

      this.monacoEditor.onDidChangeCursorPosition(e => {
        console.log('Curson change', e)
        const position = this.pathFinder.getPath(e.position.lineNumber, e.position.column);
        console.log('New position: ' + position)
      });
      this.monacoEditor.onDidChangeModelContent(e => {
        console.log('Model change');
        this.pathFinder = new JSONPathFinder(this.monacoEditor.getModel().getValue());
      })
    }
  }
}
