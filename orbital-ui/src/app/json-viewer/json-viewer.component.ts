import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostBinding,
  Input,
  OnDestroy,
  ViewChild
} from '@angular/core';
import {editor} from 'monaco-editor';


// Import the core monaco editor
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';

import {JSONPathFinder} from 'src/app/json-viewer/JsonPathFinder';
import {Clipboard} from '@angular/cdk/clipboard';
import {JsonTypeInlayHintProvider} from "./JsonTypeInlayHintProvider";
import {isNullOrUndefined} from "../utils/utils";
import { isSourceWithTypeHints, SourceWithTypeHints } from './json-results-view.component';
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import ITextModel = editor.ITextModel;


@Component({
  selector: 'app-json-viewer',
  template: `
    <app-panel-header *ngIf="showHeader" [title]="title" [isSecondary]="true">
      <tui-checkbox-labeled [(ngModel)]="showTypeHints" *ngIf="hasTypes">
        Show types
      </tui-checkbox-labeled>
      <tui-notification *ngIf="showResultsSizeWarning || isResponseLarge" status="warning" class="alert">
        The response is really big.
        <ng-container *ngIf="isResponseLarge">Some features have been disabled. </ng-container>
        <ng-container *ngIf="showResultsSizeWarning">Only showing [x] number of results. </ng-container>
      </tui-notification>
      <div *ngIf="!showResultsSizeWarning && !isResponseLarge" class="spacer"></div>
      <button (click)="applyFormat()" tuiButton size="s" appearance="outline">Format</button>
      <button (click)="copyToClipboard()" tuiButton size="s" appearance="outline">{{ copyButtonText }}</button>
      </app-panel-header>
    <div #codeEditorContainer class="code-editor"></div>`,
  styleUrls: ['./json-viewer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class JsonViewerComponent implements OnDestroy {

  private _json: string | SourceWithTypeHints;
  private pathFinder: JSONPathFinder
  copyButtonText = 'Copy'
  @Input()
  title: string;
  // Prevents tooltip displaying in browser
  @HostBinding('attr.title') get getTitle(): null {
    return null;
  }
  @Input()
  showHeader = true;

  @Input()
  showResultsSizeWarning: boolean;

  @Input()
  isResponseLarge: boolean;

  private hintProvider: JsonTypeInlayHintProvider = new JsonTypeInlayHintProvider();

  constructor(private changeDetector: ChangeDetectorRef, private clipboard: Clipboard) {
    monaco.languages.registerInlayHintsProvider("json", this.hintProvider)
  }

  get hasTypes(): Boolean {
    return !isNullOrUndefined(this._json) && typeof this._json !== 'string' && (this._json as SourceWithTypeHints).typeHints.length > 0;
  }

  private _showTypeHints: Boolean = true;
  get showTypeHints(): Boolean {
    return this._showTypeHints;
  }
  set showTypeHints(value) {
    this._showTypeHints = value;
    this.updateHintsIfPossible()
    this.changeDetector.markForCheck();
  }

  private _readOnly: boolean = false;
  @Input()
  get readOnly(): boolean {
    return this._readOnly;
  }

  set readOnly(value) {
    this._readOnly = value;
    this.createOrUpdateEditor();
  }

  get jsonString(): string {
    if (isNullOrUndefined(this._json)) {
      return null;
    }
    if (typeof this._json === 'string') {
      return this._json;
    } else {
      return (this._json as SourceWithTypeHints).source
    }
  }

  @Input()
  get json(): string | SourceWithTypeHints {
    return this._json;
  }

  set json(value: string | SourceWithTypeHints) {
    if (value === this._json || value === "") {
      return;
    }
    this._json = value;
    try {
      this.pathFinder = isSourceWithTypeHints(value) ? new JSONPathFinder(this.jsonString) : null;
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
    const val = this.monacoEditor.getValue();
    const json = JSON.stringify(JSON.parse(val), null, 3);
    this.monacoEditor.setValue(json);
    this.changeDetector.markForCheck();
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

  modelUri: monaco.Uri = null;

  private createOrUpdateEditor(): void {
    if (!this.codeEditorContainer) {
      return;
    }
    if (isNullOrUndefined(this.json)) {
      return;
    }
    const monacoTextModel = this.createNewTextModel();
    this.updateHintsIfPossible()


    if (this.monacoEditor) {
      this.monacoEditor.setModel(monacoTextModel)
    } else {
      this.monacoEditor = monaco.editor.create(this._codeEditorContainer.nativeElement, {
        model: monacoTextModel,
        glyphMargin: true,
        automaticLayout: true,
        readOnly: this._readOnly,
        contextmenu: true,
        folding: true,
        codeLens: true,
        inlayHints: {
          fontFamily: 'nunito-sans',
          fontSize: 13,
          enabled: 'on'
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

  private createNewTextModel(): ITextModel {
    this.modelUri = monaco.Uri.parse(`inmemory://json-file-${Date.now()}.json`); // a made up unique URI for our model
    return monaco.editor.createModel(this.jsonString, 'json', this.modelUri)
  }

  private updateHintsIfPossible() {
    if (!this.modelUri) return;
    if (isNullOrUndefined(this.json)) return
    if (typeof this.json === 'string') {
      // Remove any lingering type hints
      this.hintProvider.setHints(this.modelUri.path, []);
    } else {
      const src = this.json as SourceWithTypeHints;
      const typeHints = this.showTypeHints ? src.typeHints : [];
      this.hintProvider.setHints(this.modelUri.path, typeHints);
    }


  }
}
