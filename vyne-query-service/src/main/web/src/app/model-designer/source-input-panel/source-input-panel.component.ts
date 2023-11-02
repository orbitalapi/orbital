import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter, Input, Output,
    ViewChild
} from '@angular/core';
import {FormControl} from "@angular/forms";
import {TuiFileLike} from "@taiga-ui/kit";
import * as monaco from "monaco-editor";
import {MonacoServices} from "monaco-languageclient";
import {editor} from "monaco-editor";
import ITextModel = editor.ITextModel;
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;
import {TAXI_LANGUAGE_ID} from "../../code-viewer/taxi-lang.monaco";
import IModelContentChangedEvent = editor.IModelContentChangedEvent;
import {debounceTime} from "rxjs/operators";

@Component({
    selector: 'app-designer-source-input-panel',
    template: `
    <app-panel-header title="Source content">
      <div class="spacer"></div>
      <button tuiButton size="s" appearance="outline" *ngIf="editorVisible" (click)="clearContent()">Clear</button>
    </app-panel-header>
    <div class="empty-state" *ngIf="!editorVisible">
      <tui-input-files
        *ngIf="!fileDropControl.value"
        [formControl]="fileDropControl"
        (reject)="onReject($event)"
      ></tui-input-files>
      <button tuiButton appearance="outline" (click)="showEditor()">Start by writing</button>
    </div>
    <div class="editor-container" #codeEditorContainer *ngIf="editorVisible">

    </div>
  `,
    styleUrls: ['./source-input-panel.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SourceInputPanelComponent {

    readonly fileDropControl = new FormControl();
    editorVisible = false;

    private monacoEditor: IStandaloneCodeEditor;
    private monacoModel: ITextModel;

    private _content: string;
    private monacoModelChanged$ = new EventEmitter<IModelContentChangedEvent>();

    @Input()
    get content(): string {
        return this._content;
    }

    set content(value: string) {
        this.updateContent(value)
    }

    private updateContent(value: string, alsoUpdateMonaco: boolean = true) {
        if (this._content === value) {
            return;
        }
        this._content = value;
        if (this.monacoModel && alsoUpdateMonaco) {
            this.monacoModel.setValue(value);
        }
        this.contentChange.emit(value);
        if (this.content !== null && this.content !== undefined) {
            this.editorVisible = true;
        }
        this.changeDetector.markForCheck();
    }

    @Output()
    contentChange = new EventEmitter<string>();


    constructor(private changeDetector: ChangeDetectorRef) {
        this.monacoModelChanged$.pipe(
            debounceTime(250),
        ).subscribe(e => {
            this.updateContent(this.monacoModel.getValue(), false);
        });


        this.fileDropControl.valueChanges.subscribe(next => {
            if (next !== null && next !== undefined) {
                this.loadFileContent(next)
            }
        })
    }

    private loadFileContent(fileLike: TuiFileLike) {
        const fileReader = new FileReader();
        fileReader.onloadend = () => {
            this.updateContent(fileReader.result as string, true);
        }
        fileReader.readAsText(fileLike as File);
    }

    onReject($event: TuiFileLike | TuiFileLike[]) {

    }

    showEditor() {
        this.editorVisible = true;
        this.changeDetector.markForCheck();
    }

    private _codeEditorContainer: ElementRef;
    @ViewChild('codeEditorContainer', {static: false})
    get codeEditorContainer(): ElementRef {
        return this._codeEditorContainer;
    }

    set codeEditorContainer(value: ElementRef) {
        this._codeEditorContainer = value;
        if (value === null || value === undefined) {
            this.destroyMonacoIfExists();
        } else {
            this.createMonacoEditor();
        }
    }

    private destroyMonacoIfExists() {
        if (this.monacoEditor) {
            this.monacoEditor.dispose();
        }
    }

    private createMonacoEditor(): void {
        this.destroyMonacoIfExists();
        this.monacoModel = monaco.editor.createModel(this.content);
        this.monacoModel.onDidChangeContent(e => this.monacoModelChanged$.next(e));
        this.monacoEditor = monaco.editor.create(this._codeEditorContainer.nativeElement, {
            model: this.monacoModel,
            glyphMargin: true,
            lightbulb: {
                enabled: true
            },
            automaticLayout: true,
        });
    }

    clearContent() {
        this.fileDropControl.setValue(null);
        this.updateContent(null, true);
        this.editorVisible = false;
        this.changeDetector.markForCheck();
    }
}
