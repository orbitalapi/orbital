import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import {isNullOrUndefined} from "../utils/utils";
import * as monaco from "monaco-editor";
import {JSONPathFinder} from "../json-viewer/JsonPathFinder";
import {editor} from "monaco-editor";
import ITextModel = editor.ITextModel;
import {
  IStandaloneCodeEditor
} from "@codingame/monaco-vscode-api/vscode/vs/editor/standalone/browser/standaloneCodeEditor";
import IModelContentChangedEvent = editor.IModelContentChangedEvent;
import {debounceTime} from "rxjs/operators";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

/**
 * This is a non-taxi editor.
 * We named our taxi editor "code editor", which was silly.
 *
 * This doesn't provide any language server, etc.
 * Use it when editing something that isn't Taxi, but you still want monaco.
 *
 * For JSON, use the dedicated json-viewer component, which provides type hints etc.
 */
@Component({
  selector: 'app-simple-code-editor',
  standalone: true,
  imports: [],
  template: `
    <div #codeEditorContainer class="code-editor"></div>
  `,
  styleUrl: './simple-code-editor.component.scss'
})
export class SimpleCodeEditorComponent implements OnDestroy{
  modelUri: monaco.Uri = null;
  private monacoEditor: IStandaloneCodeEditor;
  private _codeEditorContainer: ElementRef;
  private modelChanged$ = new EventEmitter<IModelContentChangedEvent>();
  constructor() {
    this.modelChanged$.pipe(
      debounceTime(150),
      takeUntilDestroyed()
    ).subscribe(async(e) => {
      this.updateContent(this.monacoEditor.getModel().getValue());
    })
  }

  @ViewChild('codeEditorContainer')
  get codeEditorContainer(): ElementRef {
    return this._codeEditorContainer;
  }

  set codeEditorContainer(value: ElementRef) {
    this._codeEditorContainer = value;
    this.createOrUpdateEditor()
  }

  updateContent(content: string) {
    if (this._content !== content) {
      this._content = content;
      this.contentChange.emit(content);
    }
  }

  private _content: string = '';
  @Input()
  get content():string {
    return this._content
  }
  set content(value) {
    if (value === this._content) {
      return;
    }
    // If we allow undefind here, the component never renders
    if (isNullOrUndefined(value)) {
      this._content = '';
    } else {
      this._content = value;
    }

    this.createOrUpdateEditor();
  }
  @Output()
  contentChange = new EventEmitter<string>();

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
    if (isNullOrUndefined(this.content)) {
      return;
    }
    const monacoTextModel: ITextModel = this.createNewTextModel();
    monacoTextModel.onDidChangeContent(() => this.modelChanged$.emit())


    if (this.monacoEditor) {
      this.monacoEditor.setModel(monacoTextModel)
    } else {
      this.monacoEditor = monaco.editor.create(this._codeEditorContainer.nativeElement, {
        model: monacoTextModel,
        language: 'json',
        glyphMargin: true,
        automaticLayout: true,
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
    }
  }

  private createNewTextModel(): ITextModel {
    this.modelUri = monaco.Uri.parse(`inmemory://json-file-${Date.now()}.json`); // a made up unique URI for our model
    return monaco.editor.createModel(this.content, 'json', this.modelUri)
  }

}
