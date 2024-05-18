import {Component, ElementRef, Inject, ViewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
  IStandaloneCodeEditor
} from "@codingame/monaco-vscode-api/vscode/vs/editor/standalone/browser/standaloneCodeEditor";
import {isNullOrUndefined} from "../../utils/utils";
import * as monaco from "monaco-editor";
import {JSONPathFinder} from "../../json-viewer/JsonPathFinder";
import {TuiButtonModule, TuiDialogContext} from "@taiga-ui/core";
import {POLYMORPHEUS_CONTEXT} from "@tinkoff/ng-polymorpheus";
import {editor} from "monaco-editor";
import ITextModel = editor.ITextModel;

@Component({
  selector: 'app-response-editor-dialog',
  standalone: true,
  imports: [CommonModule, TuiButtonModule],
  template: `
    <div #codeEditorContainer class="code-editor"></div>
    <div class="row">
      <button tuiButton size="m" appearance="outline" (click)="context.completeWith(context.data)">Cancel</button>
      <span class="spacer"></span>
      <button tuiButton size="m" appearance="primary" (click)="update()">Update</button>
    </div>
  `,
  styleUrls: ['./response-editor-dialog.component.scss']
})
export class ResponseEditorDialogComponent {

  constructor(@Inject(POLYMORPHEUS_CONTEXT)
              readonly context: TuiDialogContext<string, string>,) {
  }

  modelUri: monaco.Uri = null;

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

  private createOrUpdateEditor(): void {
    if (!this.codeEditorContainer) {
      return;
    }
    const monacoTextModel = this.createNewTextModel();


    if (this.monacoEditor) {
      this.monacoEditor.setModel(monacoTextModel)
    } else {
      this.monacoEditor = monaco.editor.create(this._codeEditorContainer.nativeElement, {
        model: monacoTextModel,
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
    }
  }

  private createNewTextModel(): ITextModel {
    this.modelUri = monaco.Uri.parse(`inmemory://json-file-${Date.now()}.json`); // a made up unique URI for our model
    return monaco.editor.createModel(this.context.data, 'json', this.modelUri)
  }

  update() {
    this.context.completeWith(this.monacoEditor.getModel().getValue())
  }
}
