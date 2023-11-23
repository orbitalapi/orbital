import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from '@angular/core';
import {editor, Uri} from "monaco-editor";
import {MonacoLanguageServerService} from "./language-server.service";
import {MonacoLanguageClient} from "monaco-languageclient";
import {debounceTime} from "rxjs/operators";
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import ITextModel = editor.ITextModel;
import IModelContentChangedEvent = editor.IModelContentChangedEvent;
// import {createModelReference} from "@codingame/monaco-vscode-api/monaco";

type WordWrapOptions = 'off' | 'on' | 'wordWrapColumn' | 'bounded';

@Component({
    selector: 'app-code-editor',
    styleUrls: ['./code-editor.component.scss'],
    template: `
        <div #codeEditorContainer class="code-editor"></div>
    `
})
export class CodeEditorComponent {

    private editorResourceUri: Uri | null = null;

    private _codeEditorContainer: ElementRef;
    @ViewChild('codeEditorContainer')
    get codeEditorContainer(): ElementRef {
        return this._codeEditorContainer;
    }

    set codeEditorContainer(value: ElementRef) {
        this._codeEditorContainer = value;
        this.createMonacoEditor()
    }

    private _actions: editor.IActionDescriptor[] = [];
    @Input()
    get actions(): editor.IActionDescriptor[] {
        return this._actions;
    }

    set actions(value) {
        if (this._actions === value) {
            return;
        }
        this._actions = value;
        if (this.monacoEditor) {
            this.updateActionsOnEditor();
        }
    }

    // private editorTheme = iplastic_theme;
    //
    private _readOnly: boolean = false;

    @Input()
    get readOnly(): boolean {
        return this._readOnly;
    }

    set readOnly(value: boolean) {
        if (value === this._readOnly) {
            return;
        }
        this._readOnly = value;
        if (this.monacoEditor) {
            this.monacoEditor.updateOptions({readOnly: this.readOnly});
        }
    }

    private _wordWrap: WordWrapOptions = 'off';

    @Input()
    get wordWrap(): WordWrapOptions {
        return this._wordWrap;
    }

    set wordWrap(value: WordWrapOptions) {
        if (value === this._wordWrap) {
            return;
        }
        this._wordWrap = value;
        if (this.monacoEditor) {
            this.monacoEditor.updateOptions({wordWrap: this.wordWrap});
        }
    }

    //
    private _content: string = '';
    @Input()
    get content(): string {
        return this._content;
    }

    set content(value: string) {
        if (this._content === value) {
            return;
        }
        this._content = value;
        if (this.monacoModel) {
            this.monacoModel.setValue(value);
            this.contentChange.emit(value);
        }
    }

    //
    private modelChanged$ = new EventEmitter<IModelContentChangedEvent>();
    //
    @Output()
    contentChange = new EventEmitter<string>();
    //
    private monacoEditor: IStandaloneCodeEditor;
    private monacoModel: ITextModel;
    // //
    private monacoLanguageClient: MonacoLanguageClient;
    private webSocket: WebSocket;

    //
    constructor(
        private languageInitServices: MonacoLanguageServerService
    ) {

        // this.languageInitServices.languageServicesInit$
        //     .subscribe(() => {
        //         // editor.defineTheme('vyne', this.editorTheme as any);
        //         editor.setTheme('vyne');
        //     })
        this.modelChanged$.pipe(
            debounceTime(250),
        ).subscribe(e => {
            this.createWebsocket();
            this.updateContent(this.monacoModel.getValue());
            if (this.webSocket.readyState != this.webSocket.OPEN) {
                console.log("Refresh websocket connection for language server");
                this.createWebsocket();
            }
        })
    }

    private createWebsocket() {
        // if (this.webSocket) {
        //     this.webSocket.close()
        // }
        // this.languageInitServices.createLanguageServerWebsocket()
        //     .subscribe(websocket => {
        //         this.webSocket = websocket;
        //
        //         // For testing websocket reconnection
        //         // @ts-ignore
        //         window.killWebsocket = () => {
        //             this.webSocket.close()
        //         }
        //     })

    }

    //
    private newQueryUri() {
        return Uri.parse(`inmemory://query-${Math.floor(Math.random() * 1_000_000_000)}.taxi`)
    }

    //
    private async createMonacoEditor() {
        if (this.monacoEditor) {
            this.monacoEditor.dispose();
        }

        this.editorResourceUri = this.newQueryUri();
        //
        // const modelReference = await createModelReference(this.editorResourceUri, this.content)
        //
        // modelReference.object.setLanguageId(TAXI_LANGUAGE_ID);
        // modelReference.object.onDidChangeContent((e: editor.IModelContentChangedEvent) => this.modelChanged$.next(e));
        //
        // this.monacoEditor = createConfiguredEditor(this.codeEditorContainer.nativeElement, {
        //     model: this.monacoModel,
        //     glyphMargin: true,
        //     lightbulb: {
        //         enabled: true
        //     },
        //     parameterHints: {
        //         enabled: true
        //     },
        //     automaticLayout: true,
        //     readOnly: this._readOnly,
        //     wordWrap: this._wordWrap,
        // });
        //
        // this.updateActionsOnEditor()
        // this.createWebsocket();
    }

    //
    private updateActionsOnEditor() {
        // this.actions.forEach(action => {
        //     this.monacoEditor.addAction(action);
        // })
    }

    updateContent(content: string) {
        // if (this._content !== content) {
        //     this._content = content;
        //     this.contentChange.emit(content);
        // }
    }

}

