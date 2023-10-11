import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  ViewChild
} from '@angular/core';
import {editor, languages} from 'monaco-editor';


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
import {JSONPathFinder} from 'src/app/json-viewer/JsonPathFinder';
import {Clipboard} from '@angular/cdk/clipboard';
import {TypePosition} from "../model-designer/taxi-parser.service";
import {JsonTypeInlayHintProvider} from "./JsonTypeInlayHintProvider";
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import ITextModel = editor.ITextModel;
import InlayHint = languages.InlayHint;
import InlayHintKind = languages.InlayHintKind;

@Component({
  selector: 'app-json-viewer',
  template: `
    <app-panel-header *ngIf="showHeader" [title]="title">
      <div class="spacer"></div>
      <button (click)="applyFormat()"
              tuiButton size="s" appearance="outline"
              [disabled]="formatInProgress">{{ formatInProgress ? 'Formatting...' : 'Format'}}</button>
      <button (click)="copyToClipboard()" tuiButton size="s" appearance="outline">{{ copyButtonText }}</button>
    </app-panel-header>
    <div #codeEditorContainer class="code-editor"></div>`,
  styleUrls: ['./json-viewer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class JsonViewerComponent implements OnDestroy {

  formatLabel = 'Format'

  private _json: string;
  private pathFinder: JSONPathFinder
  formatInProgress = false;
  copyButtonText = 'Copy'
  @Input()
  title: string;
  @Input()
  showHeader = true;

  private _typeHints: TypePosition[] | null = null;
  private inlayHints: InlayHint[] = null;

  constructor(private changeDetector: ChangeDetectorRef, private clipboard: Clipboard) {
    // This does nothing, but prevents tree-shaking
    const features = [monadoEditorAll, monacoFeature4, monacoFeature5, monacoFeature6, monacoFeature7, monacoFeature8, languageFeatureService];

  }

  @Input()
  get typeHints(): TypePosition[] | null {
    return this._typeHints;
  }

  set typeHints(value: TypePosition[] | null) {
    if (value === this._typeHints) {
      return;
    }
    this._typeHints = value;
    this.inlayHints = this.typeHints.map(hint => {
      return {
        kind: InlayHintKind.Type,
        label: hint.type.shortDisplayName,
        position: {
          column: hint.start.char,
          lineNumber: hint.start.line
        },
        tooltip: hint.type.longDisplayName,
        paddingLeft: true
      }
    })
    if (this.modelUri) {
      JsonTypeInlayHintProvider.hints.set(this.modelUri, this.inlayHints);
    }
  }

  private _readOnly: boolean = false;
  @Input()
  get readOnly():boolean {
    return this._readOnly;
  }
  set readOnly(value) {
    this._readOnly = value;
    this.createOrUpdateEditor();
  }
  @Input()
  get json(): string {
    return this._json;
  }

  set json(value: string) {
    if (value === this._json) {
      return;
    }
    this._json = value;
    try {
      this.pathFinder = new JSONPathFinder(this.json)
    } catch (e) {
      console.error(`Failed to build JSON Path finder: ${e}`)
      this.pathFinder = null;
    }

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

  applyFormat() {
    this.formatInProgress = true;
    this.changeDetector.markForCheck();
    this.monacoEditor.getAction('editor.action.formatDocument')
      .run()
      .then(result => {
        this.formatInProgress = false;
        this.changeDetector.markForCheck();
      })
  }

  copyToClipboard() {
    this.monacoEditor.getAction('editor.action.clipboardCopyAction')
      .run()
      .then(() => {
        this.copyButtonText = 'Copied';
        this.changeDetector.markForCheck();
        setTimeout(() => {
          this.copyButtonText = 'Copy';
          this.changeDetector.markForCheck();
        }, 2000);
      });
  }

  modelUri:monaco.Uri = null;
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
        this.modelUri = monaco.Uri.parse(`inmemory://json-file-${Date.now()}.json`); // a made up unique URI for our model
        if (this.inlayHints) {
          JsonTypeInlayHintProvider.hints.set(this.modelUri, this.inlayHints)
        }
        monacoTextModel = monaco.editor.createModel(this.json, 'json', this.modelUri)
      }


      const _typehints = this.typeHints || [];
      monaco.languages.registerInlayHintsProvider("json", new JsonTypeInlayHintProvider())

      this.monacoEditor = monaco.editor.create(this._codeEditorContainer.nativeElement, {
        model: monacoTextModel,
        glyphMargin: true,
        automaticLayout: true,
        readOnly: this._readOnly,
        contextmenu: true,
        folding: true,
        codeLens: true,
        inlayHints: {
          enabled: true
        }
      });


      // Add back the "Copy" action.  Shouldn't have to do this.
      // but is a workaround as documented here:
      // https://github.com/microsoft/monaco-editor/issues/2195#issuecomment-711471692
      this.monacoEditor.addAction({
        id: 'editor.action.clipboardCopyAction',
        label: 'Copy',
        run: () => {
          this.monacoEditor?.focus()
          document.execCommand('copy')
        },
      })

      this.monacoEditor.onDidChangeCursorPosition(e => {
        if (this.pathFinder != null) {
          const position = this.pathFinder.getPath(e.position.lineNumber, e.position.column);
          console.log('New position: ' + position)
        }
      });
      this.monacoEditor.onDidChangeModelContent(e => {
        if (this.pathFinder != null) {
          console.log('Model change');
          this.pathFinder = new JSONPathFinder(this.monacoEditor.getModel().getValue());
        }
      })
    }
  }
}
